package com.infotel.seleniumrobot.grid.distributor;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.Comparator;
import java.util.Set;
import java.util.logging.Logger;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.grid.config.Config;
import org.openqa.selenium.grid.data.NodeStatus;
import org.openqa.selenium.grid.data.Slot;
import org.openqa.selenium.grid.data.SlotId;
import org.openqa.selenium.grid.distributor.local.LocalDistributor;
import org.openqa.selenium.grid.distributor.selector.SlotSelector;

import com.google.common.annotations.VisibleForTesting;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClient;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

public class SeleniumRobotSlotSelector implements SlotSelector {

	private static final Logger LOG = Logger.getLogger(SeleniumRobotSlotSelector.class.getName());

	public static SlotSelector create(Config config) {
	    return new SeleniumRobotSlotSelector();
	  }

	@Override
	public Set<SlotId> selectSlot(Capabilities capabilities, Set<NodeStatus> nodes) {

		// First, filter the Nodes that support the required capabilities. Then, the filtered Nodes
		// get ordered in ascendant order by the number of browsers they support.
		// With this, Nodes with diverse configurations (supporting many browsers, e.g. Chrome,
		// Firefox, Safari) are placed at the bottom so they have more availability when a session
		// requests a browser supported only by a few Nodes (e.g. Safari only supported on macOS
		// Nodes).
		// After that, Nodes are ordered by their load, last session creation, and their id.
		return nodes.stream()
	      .filter(node -> node.hasCapacity(capabilities))
	      .filter(node -> acceptNewSession(node, capabilities))
	      .sorted(
	        Comparator.comparingLong(this::getNumberOfSupportedBrowsers)
	        // Now sort by node which has the lowest load (natural ordering)
	          .thenComparingDouble(NodeStatus::getLoad)
	          // Then last session created (oldest first), so natural ordering again
	          .thenComparingLong(NodeStatus::getLastSessionCreated)
	          // And use the node id as a tie-breaker.
	          .thenComparing(NodeStatus::getNodeId))
	      .flatMap(node -> node.getSlots().stream()
	        .filter(slot -> slot.getSession() == null)
	        .filter(slot -> slot.isSupporting(capabilities))
	        .map(Slot::getId))
	      .collect(toImmutableSet());
	  }

	/**
	 * is the node accepting new session
	 * On node startup, we define a maxSession (number of test sessions in parallel) which is different from selenium maxSession (number of browsers in parallel)
	 * as we allow attaching to existing browsers inside the same test session
	 * @return
	 */
	private boolean acceptNewSession(NodeStatus node, Capabilities capabilities) {
		int maxTestSessions = 0;
		try {
			maxTestSessions = new NodeStatusServletClient(node.getExternalUri().getHost(), node.getExternalUri().getPort()).getStatus().getMaxSessions();
		} catch (Exception e) {
			LOG.fine("Cannot get max sessions from node");
		  	return false;
		}
		long sessions = node.getSlots().stream().filter(slot -> slot.getSession() != null).count();

		// do not accept new sessions if the number of test sessions is reached and 'attachSessionOnNode' capability does not correspond to this node
		if (sessions >= maxTestSessions
			&& (capabilities.getCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE) == null
			|| !capabilities.getCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE).toString().equals(node.getExternalUri().toString()))
		) {
			LOG.fine(String.format("Max session reached for node %s", node.getExternalUri()));
			return false;
		}
		LOG.fine(String.format("Slots available for node %s", node.getExternalUri()));
		return true;
	}
	  

	  @VisibleForTesting
	  long getNumberOfSupportedBrowsers(NodeStatus nodeStatus) {
	    return nodeStatus.getSlots()
	      .stream()
	      .map(slot -> slot.getStereotype().getBrowserName().toLowerCase())
	      .distinct()
	      .count();
	  }

}
