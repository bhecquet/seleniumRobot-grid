package com.infotel.seleniumrobot.grid.tests.aspects;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.data.Availability;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.data.NodeId;
import org.openqa.selenium.grid.data.NodeStatus;
import org.openqa.selenium.grid.data.Slot;
import org.openqa.selenium.grid.data.SlotId;
import org.openqa.selenium.grid.node.local.LocalNode;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.aspects.NodeActions;
import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.GridStatus;

import static org.mockito.Mockito.*;

public class TestNodeActions extends BaseMockitoTest {

	@Mock
	CreateSessionRequest createSessionRequest;
	
	@Mock
	ProceedingJoinPoint joinPoint;
	
	@Mock
	LocalNode localNode;
	
	@Mock
	NodeActions nodeActions;
	
	@Mock
	LaunchConfig currentLaunchConfig;
	
	@Mock
	GridNodeConfiguration currentNodeConfig;

	private MockedStatic mockedLaunchConfig;

	@BeforeMethod(groups={"grid"})
	public void setup() throws Exception {
		mockedLaunchConfig = mockStatic(LaunchConfig.class);

		mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(currentLaunchConfig);
		mockedLaunchConfig.when(() -> LaunchConfig.getCurrentNodeConfig()).thenReturn(currentNodeConfig);
		
		when(joinPoint.getArgs()).thenReturn(new Object[] {createSessionRequest}); // to mock 'onNewSession'
		when(joinPoint.getThis()).thenReturn(localNode);
		when(localNode.getExternalUri()).thenReturn(new URI("http://myHost:5555"));
		
		nodeActions = spy(new NodeActions());
	}

	@AfterMethod(groups = "grid", alwaysRun = true)
	private void closeMocks() {
		mockedLaunchConfig.close();
	}

	@Test(groups={"grid"})
	public void testGetStatus() throws Throwable {
		UUID nodeUuid = UUID.randomUUID();
		Slot slot = new Slot(
						new SlotId(new NodeId(nodeUuid), nodeUuid),
						new MutableCapabilities(Map.of("browserName", "chrome")),
						Instant.now(),
				null);
		when(currentNodeConfig.getStatus()).thenReturn(GridStatus.ACTIVE);
		when(joinPoint.proceed(any())).thenReturn(new NodeStatus(new NodeId(nodeUuid),
				new URI("http://localhost:5555"),
				1,
				Set.of(slot),
				Availability.UP,
				Duration.ofSeconds(10),
				Duration.ofSeconds(10),
				"1.0",
				new HashMap<>()));
		when(currentLaunchConfig.getDevMode()).thenReturn(false);
		
		NodeStatus nodeStatus = (NodeStatus) nodeActions.onGetStatus(joinPoint);
		Assert.assertEquals(nodeStatus.getAvailability(), Availability.UP);
	}

	/**
	 * When node is inactive, Availablity is set to "DRAINING" so that no new test session can be started
	 * @throws Throwable
	 */
	@Test(groups={"grid"})
	public void testGetStatusNodeInactive() throws Throwable {
		UUID nodeUuid = UUID.randomUUID();
		Slot slot = new Slot(
						new SlotId(new NodeId(nodeUuid), nodeUuid),
						new MutableCapabilities(Map.of("browserName", "chrome")),
						Instant.now(),
				null);
		when(currentNodeConfig.getStatus()).thenReturn(GridStatus.INACTIVE);
		when(joinPoint.proceed(any())).thenReturn(new NodeStatus(new NodeId(nodeUuid),
				new URI("http://localhost:5555"),
				1,
				Set.of(slot),
				Availability.UP,
				Duration.ofSeconds(10),
				Duration.ofSeconds(10),
				"1.0",
				new HashMap<>()));
		when(currentLaunchConfig.getDevMode()).thenReturn(false);

		NodeStatus nodeStatus = (NodeStatus) nodeActions.onGetStatus(joinPoint);
		Assert.assertEquals(nodeStatus.getAvailability(), Availability.DRAINING);
	}

}
