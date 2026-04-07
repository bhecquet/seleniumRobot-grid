package com.infotel.seleniumrobot.grid.distributor;

import com.google.common.annotations.VisibleForTesting;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.grid.config.Config;
import org.openqa.selenium.grid.data.*;
import org.openqa.selenium.grid.distributor.selector.SlotSelector;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.openqa.selenium.grid.data.Availability.UP;

/**
 * Select slot based on custom capabilities
 * - attachToNode
 * - allowAdditionalSessionsOnNode
 * <p>
 * /!\ THIS REQUIRES '--newsession-threadpool-size' to be set to '1' so that looking at currently running sessions
 * really reflects the node status
 * There are cases where 2 tests launched at the same time will 'see' node to not be busy at all and so allow slot to be used
 */
public class SeleniumRobotSlotSelector implements SlotSelector {

    private static final Logger LOG = Logger.getLogger(SeleniumRobotSlotSelector.class.getName());

    public static SlotSelector create(Config config) {
        return new SeleniumRobotSlotSelector();
    }

    @Override
    public Set<SlotId> selectSlot(Capabilities capabilities, Set<NodeStatus> nodes, SlotMatcher slotMatcher) {

        // First, filter the Nodes that support the required capabilities. Then, the filtered Nodes
        // get ordered in ascendant order by the number of browsers they support.
        // With this, Nodes with diverse configurations (supporting many browsers, e.g. Chrome,
        // Firefox, Safari) are placed at the bottom so they have more availability when a session
        // requests a browser supported only by a few Nodes (e.g. Safari only supported on macOS
        // Nodes).
        // After that, Nodes are ordered by their load, last session creation, and their id.
        return nodes.stream()
                .filter(node -> node.hasCapacity(capabilities, slotMatcher) && node.getAvailability() == UP)
                .filter(node -> acceptNewSession(node, capabilities))
                .sorted(
                        Comparator.comparingLong(this::getNumberOfSupportedBrowsers)
                                // Now sort by node which has the lowest load (natural ordering)
                                .thenComparingDouble(NodeStatus::getLoad)
                                // Then last session created (oldest first), so natural ordering again
                                .thenComparingLong(NodeStatus::getLastSessionCreated)
                                // Then sort by stereotype browserVersion (descending order). SemVer comparison with
                                // considering empty value at first.
                                .thenComparing(
                                        NodeStatus::getBrowserVersion, new SemanticVersionComparator().reversed())
                                // And use the node id as a tie-breaker.
                                .thenComparing(NodeStatus::getNodeId))
                .flatMap(node -> node.getSlots().stream()
                        .filter(slot -> slot.getSession() == null)
                        .filter(slot -> slot.isSupporting(capabilities, slotMatcher))
                        .map(Slot::getId))
                .collect(toImmutableSet());
    }

    /**
     * is the node accepting new session
     * On node startup, we define a maxSession (number of test sessions in parallel) which is different from selenium maxSession (number of browsers in parallel)
     * as we allow attaching to existing browsers inside the same test session
     *
     * @return true if the node accepts a new session
     */
    private boolean acceptNewSession(NodeStatus nodeStatus, Capabilities capabilities) {
        int maxTestSessions = 0;
        try {
            maxTestSessions = Integer.parseInt(nodeStatus.getSlots().stream().findFirst().get().getStereotype().getCapability(LaunchConfig.MAX_SESSIONS).toString());
        } catch (Exception e) {
            // in case there is no slot
            return false;
        }

        long sessions = nodeStatus.getSlots().stream().filter(slot -> slot.getSession() != null).count();

        // allow more sessions expects that we already checked that maximum node capacity has not been reached
        boolean allowMoreSessions = nodeStatus.getSlots().stream()
                .filter(slot -> slot.getSession() != null)
                .filter(slot -> slot.getSession().getCapabilities().getCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE) != null
                        && (Boolean) slot.getSession().getCapabilities().getCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE))
                .count() == sessions;

        // do not accept new sessions if the number of test sessions is reached and 'attachSessionOnNode' capability does not correspond to this node
        if (sessions >= maxTestSessions
                && (capabilities.getCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE) == null
                || !capabilities.getCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE).toString().equals(nodeStatus.getExternalUri().toString()))
                && (capabilities.getCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE) == null
                || !(Boolean) capabilities.getCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE)
                || !allowMoreSessions)
        ) {
            LOG.fine(String.format("Max session reached for node %s", nodeStatus.getExternalUri()));
            return false;
        }

        LOG.fine(String.format("Slots available for node %s", nodeStatus.getExternalUri()));
        return true;
    }


    @VisibleForTesting
    long getNumberOfSupportedBrowsers(NodeStatus nodeStatus) {
        return nodeStatus.getSlots()
                .stream()
                .map(slot -> slot.getStereotype().getBrowserName().toLowerCase(Locale.ENGLISH))
                .distinct()
                .count();
    }

}
