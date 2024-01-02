package com.infotel.seleniumrobot.grid.tests.distributor;

import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.grid.data.*;
import org.openqa.selenium.remote.SessionId;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotSelector;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;

public class TestSeleniumRobotSlotSelector extends BaseMockitoTest {

	private NodeStatus nodeStatusNoSession;
	private NodeStatus nodeStatusWithSession;
	private NodeStatus nodeStatusDraining;
	
	@BeforeMethod(groups={"grid"})
	public void init() throws Exception {
		MutableCapabilities stereotype = new ChromeOptions();
		stereotype.setCapability("platformName", Platform.WIN10);
		stereotype.setCapability(LaunchConfig.NODE_URL, "http://localhost:5555"); // is always present
		stereotype.setCapability(LaunchConfig.MAX_SESSIONS, "1"); // is always present

		Slot slotNoSession = new Slot(new SlotId(new NodeId(new UUID(1234L, 1L)), new UUID(1L, 1L)),
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
				"4.8.3", 
				new HashMap<>());
		nodeStatusDraining = new NodeStatus(new NodeId(new UUID(1234L, 1L)),
				new URI("http://localhost:5555"),
				1,
				slotsNoSession,
				Availability.DRAINING,
				Duration.ofSeconds(120),
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
		Set<Slot> slotsWithSession = new HashSet<Slot>();
		slotsWithSession.add(slotWithSession);
		slotsWithSession.add(slotNoSession);

		nodeStatusWithSession = new NodeStatus(new NodeId(new UUID(1234L, 1L)),
				new URI("http://localhost:5555"),
				2,
				slotsWithSession,
				Availability.UP,
				Duration.ofSeconds(120),
				"4.8.3",
				new HashMap<>());
	}
	
	@Test(groups={"grid"})
	public void testSelectSlot() {

		Set<NodeStatus> nodeStatuses = new HashSet<NodeStatus>();
		nodeStatuses.add(nodeStatusNoSession);
		
		MutableCapabilities caps = new ChromeOptions();
		
		Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
		Assert.assertEquals(slotIds.size(), 1);
	}


	@Test(groups={"grid"})
	public void testSelectSlotInUse() throws URISyntaxException {

		Set<NodeStatus> nodeStatuses = new HashSet<NodeStatus>();
		nodeStatuses.add(nodeStatusWithSession);

		MutableCapabilities caps = new ChromeOptions();

		Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
		Assert.assertEquals(slotIds.size(), 0);
	}

	/**
	 * Check it's possible to attach to an existing browser even if max test session count is reached
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testSelectSlotInUseAndAttachToNode() throws URISyntaxException {
		// 2 sessions allowed (2 slots), but node configuration expects at most 1 test session (sr:maxSessions=1)
		Set<NodeStatus> nodeStatuses = new HashSet<NodeStatus>();
		nodeStatuses.add(nodeStatusWithSession);

		MutableCapabilities caps = new ChromeOptions();
		caps.setCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://localhost:5555");

		Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
		Assert.assertEquals(slotIds.size(), 1);
	}
	@Test(groups={"grid"})
	public void testSelectSlotInUseAndAttachToOtherNode() throws URISyntaxException {
		// 2 sessions allowed (2 slots), but node configuration expects at most 1 test session
		Set<NodeStatus> nodeStatuses = new HashSet<NodeStatus>();
		nodeStatuses.add(nodeStatusWithSession);

		MutableCapabilities caps = new ChromeOptions();
		caps.setCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://localhost:5556");

		Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
		Assert.assertEquals(slotIds.size(), 0);
	}

	/**
	 * When node is marked as INACTIVE, do not use it
	 */
	@Test(groups={"grid"})
	public void testSelectSlotInactive() {

		Set<NodeStatus> nodeStatuses = new HashSet<NodeStatus>();
		nodeStatuses.add(nodeStatusDraining);

		MutableCapabilities caps = new ChromeOptions();

		Set<SlotId> slotIds = new SeleniumRobotSlotSelector().selectSlot(caps, nodeStatuses, new SeleniumRobotSlotMatcher());
		Assert.assertEquals(slotIds.size(), 0);
	}
}
