package com.infotel.seleniumrobot.grid.tests.distributor;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher;
import com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotSelector;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.grid.data.*;
import org.openqa.selenium.remote.SessionId;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestSeleniumRobotSlotSelector extends BaseMockitoTest {

    private NodeStatus nodeStatusNoSession;
    private NodeStatus nodeStatusWithSession;
    private NodeStatus nodeStatusWithSessionAllowingOtherSession;
    private NodeStatus nodeStatusDraining;
    private MutableCapabilities stereotype;
    private Slot slotNoSession;

    @BeforeMethod(groups = {"grid"})
    public void init() throws Exception {
        stereotype = new ChromeOptions();
        stereotype.setCapability("platformName", Platform.WIN10);
        stereotype.setCapability(LaunchConfig.NODE_URL, "http://localhost:5555"); // is always present
        stereotype.setCapability(LaunchConfig.MAX_SESSIONS, "1"); // is always present

        slotNoSession = new Slot(new SlotId(new NodeId(new UUID(1234L, 1L)), new UUID(1L, 1L)),
                stereotype,
                Instant.now(),
                null);
        Set<Slot> slotsNoSession = new HashSet<Slot>();
        slotsNoSession.add(slotNoSession);

        nodeStatusNoSession = new NodeStatus(new NodeId(new UUID(1234L, 1L)),
                new URI("http://localhost:5555"),
                1,
                slotsNoSession,
                Availability.UP,
                Duration.ofSeconds(120),
                Duration.ofSeconds(300),
                "4.8.3",
                new HashMap<>());
        nodeStatusDraining = new NodeStatus(new NodeId(new UUID(1234L, 1L)),
                new URI("http://localhost:5555"),
                1,
                slotsNoSession,
                Availability.DRAINING,
                Duration.ofSeconds(120),
                Duration.ofSeconds(300),
                "4.8.3",
                new HashMap<>());

        Slot slotWithSession = new Slot(new SlotId(new NodeId(new UUID(1235L, 1L)), new UUID(1L, 1L)),
                stereotype,
                Instant.now(),
                new Session(new SessionId("1234L"),
                        new URI("http://localhost:5555"),
                        new ChromeOptions(),
                        new ChromeOptions(),
                        Instant.now()));

        Set<Slot> slotsWithSession = new HashSet<>();
        slotsWithSession.add(slotWithSession);
        slotsWithSession.add(slotNoSession);

        nodeStatusWithSession = new NodeStatus(new NodeId(new UUID(1234L, 1L)),
                new URI("http://localhost:5555"),
                2,
                slotsWithSession,
                Availability.UP,
                Duration.ofSeconds(120),
                Duration.ofSeconds(300),
                "4.8.3",
                new HashMap<>());

        MutableCapabilities capabilities = new ChromeOptions();
        capabilities.setCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, true);
        Slot slotWithSessionAllowingAdditionalSessions = new Slot(new SlotId(new NodeId(new UUID(1235L, 1L)), new UUID(1L, 1L)),
                stereotype,
                Instant.now(),
                new Session(new SessionId("1234L"),
                        new URI("http://localhost:5555"),
                        new ChromeOptions(),
                        capabilities,
                        Instant.now()));

        Set<Slot> slotsWithSessionAllowingOtherSession = new HashSet<>();
        slotsWithSessionAllowingOtherSession.add(slotWithSessionAllowingAdditionalSessions);
        slotsWithSessionAllowingOtherSession.add(slotNoSession);
        nodeStatusWithSessionAllowingOtherSession = new NodeStatus(new NodeId(new UUID(1234L, 1L)),
                new URI("http://localhost:5555"),
                2,
                slotsWithSessionAllowingOtherSession,
                Availability.UP,
                Duration.ofSeconds(120),
                Duration.ofSeconds(300),
                "4.8.3",
                new HashMap<>());
    }

    @Test(groups = {"grid"})
    public void testSelectSlot() {

        Set<NodeStatus> nodeStatuses = new HashSet<NodeStatus>();
        nodeStatuses.add(nodeStatusNoSession);

        MutableCapabilities caps = new ChromeOptions();

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 1);
    }


    @Test(groups = {"grid"})
    public void testSelectSlotInUse() {

        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSession);

        MutableCapabilities caps = new ChromeOptions();

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 0);
    }

    /**
     * Check it's possible to attach to an existing browser even if max test session count is reached
     */
    @Test(groups = {"grid"})
    public void testSelectSlotInUseAndAttachToNode() {
        // 2 sessions allowed (2 slots), but node configuration expects at most 1 test session (sr:maxSessions=1)
        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSession);

        MutableCapabilities caps = new ChromeOptions();
        caps.setCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://localhost:5555");

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 1);
    }

    @Test(groups = {"grid"})
    public void testSelectSlotInUseAndAttachToOtherNode() {
        // 2 sessions allowed (2 slots), but node configuration expects at most 1 test session
        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSession);

        MutableCapabilities caps = new ChromeOptions();
        caps.setCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://localhost:5556");

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 0);
    }

    /**
     * If the running tests allow additional sessions, and the requested capabilities also allow additional session, slot will be proposed
     * 2 sessions allowed (2 slots), node configuration expects at most 1 test session but both current test on used slot allows additional tests
     */
    @Test(groups = {"grid"})
    public void testSelectSlotInUseAndAllowAdditionalSession() {

        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSessionAllowingOtherSession);

        MutableCapabilities caps = new ChromeOptions();
        caps.setCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, true);

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 1);
    }

    /**
     * As current running test does not allow concurrent session refuse it
     */
    @Test(groups = {"grid"})
    public void testSelectSlotInUseAndDoesNotAllowAdditionalSessions() {

        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSession);

        MutableCapabilities caps = new ChromeOptions();
        caps.setCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, true);

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 0);
    }

    /**
     * As current running test does not allow concurrent session refuse it
     */
    @Test(groups = {"grid"})
    public void testSelectSlotInUseAndDoesNotAllowAdditionalSessions2() throws URISyntaxException {

        MutableCapabilities capabilities = new ChromeOptions();
        capabilities.setCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, false);
        Slot slotWithSessionNotAllowingAdditionalSessions = new Slot(new SlotId(new NodeId(new UUID(1235L, 1L)), new UUID(1L, 1L)),
                stereotype,
                Instant.now(),
                new Session(new SessionId("1234L"),
                        new URI("http://localhost:5555"),
                        new ChromeOptions(),
                        capabilities,
                        Instant.now()));

        Set<Slot> slotsWithSessionNotAllowingOtherSession = new HashSet<>();
        slotsWithSessionNotAllowingOtherSession.add(slotWithSessionNotAllowingAdditionalSessions);
        slotsWithSessionNotAllowingOtherSession.add(slotNoSession);
        NodeStatus nodeStatusWithSessionNotAllowingOtherSession = new NodeStatus(new NodeId(new UUID(1234L, 1L)),
                new URI("http://localhost:5555"),
                2,
                slotsWithSessionNotAllowingOtherSession,
                Availability.UP,
                Duration.ofSeconds(120),
                Duration.ofSeconds(300),
                "4.8.3",
                new HashMap<>());

        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSessionNotAllowingOtherSession);

        MutableCapabilities caps = new ChromeOptions();
        caps.setCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, true);

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 0);
    }

    /**
     * Test that wants to execute does not allow concurrent sessions
     */
    @Test(groups = {"grid"})
    public void testSelectSlotInUseAndAllowAdditionalSession2() {

        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSessionAllowingOtherSession);

        MutableCapabilities caps = new ChromeOptions();
        caps.setCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, false);

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 0);
    }

    /**
     *
     * Test that wants to execute does not specify anything about concurrent sessions
     */
    @Test(groups = {"grid"})
    public void testSelectSlotInUseAndAllowAdditionalSessionWithoutCapabilityFromTest() {

        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusWithSessionAllowingOtherSession);

        MutableCapabilities caps = new ChromeOptions();

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 0);
    }

    /**
     * When node is marked as INACTIVE, do not use it
     */
    @Test(groups = {"grid"})
    public void testSelectSlotInactive() {

        Set<NodeStatus> nodeStatuses = new HashSet<>();
        nodeStatuses.add(nodeStatusDraining);

        MutableCapabilities caps = new ChromeOptions();

        Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
        Assert.assertEquals(slotIds.size(), 0);
    }
}
