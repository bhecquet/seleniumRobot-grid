package com.infotel.seleniumrobot.grid.tests.aspects;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.mockito.Mock;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.node.local.LocalNode;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.aspects.NodeActions;
import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

@PrepareForTest({LaunchConfig.class})
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
	

	@BeforeMethod(groups={"grid"})
	public void setup() throws Exception {
		PowerMockito.mockStatic(LaunchConfig.class);

		PowerMockito.when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(currentLaunchConfig);
		PowerMockito.when(LaunchConfig.getCurrentNodeConfig()).thenReturn(currentNodeConfig);
		
		when(joinPoint.getArgs()).thenReturn(new Object[] {createSessionRequest}); // to mock 'onNewSession'
		when(joinPoint.getThis()).thenReturn(localNode);
		when(localNode.getExternalUri()).thenReturn(new URI("http://myHost:5555"));
		
		nodeActions = spy(new NodeActions());
	}
	
//	@Test(groups={"grid"})
//	public void testOnNewSessionMaxSessionCountNotReached() throws Throwable {
//
//		when(currentLaunchConfig.getMaxSessions()).thenReturn(2);
//		when(localNode.getCurrentSessionCount()).thenReturn(1);
//
//		nodeActions.onNewSession(joinPoint);
//		verify(joinPoint).proceed(new Object[] {createSessionRequest}); // check we use the new session request
//	}
//
//	/**
//	 * In case we attach a browser on the current node, we can overcome the max session limit
//	 * @throws Throwable
//	 */
//	@Test(groups={"grid"})
//	public void testOnNewSessionMaxSessionCountReachedAndAttachToNode() throws Throwable {
//
//		Map<String, Object> caps = new HashMap<>();
//		caps.put(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://myHost:5555");
//		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
//
//		when(currentLaunchConfig.getMaxSessions()).thenReturn(2);
//		when(localNode.getCurrentSessionCount()).thenReturn(2);
//
//		nodeActions.onNewSession(joinPoint);
//		verify(joinPoint).proceed(new Object[] {createSessionRequest}); // check we use the new session request
//	}
//
//	/**
//	 * In case we attach a browser on an other node, we can NOT overcome the max session limit
//	 * @throws Throwable
//	 */
//	@Test(groups={"grid"})
//	public void testOnNewSessionMaxSessionCountReachedAndNoAttachToNode() throws Throwable {
//
//		Map<String, Object> caps = new HashMap<>();
//		caps.put(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://myHostOther:5555");
//		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
//
//		when(currentLaunchConfig.getMaxSessions()).thenReturn(2);
//		when(localNode.getCurrentSessionCount()).thenReturn(2);
//
//		nodeActions.onNewSession(joinPoint);
//		verify(joinPoint, never()).proceed(new Object[] {createSessionRequest}); // check we do not continue the new session request
//	}
//
//	/**
//	 * In case we do not attach a browser on an other node, we can NOT overcome the max session limit
//	 * @throws Throwable
//	 */
//	@Test(groups={"grid"})
//	public void testOnNewSessionMaxSessionCountReachedAndNoAttachToNode2() throws Throwable {
//
//		Map<String, Object> caps = new HashMap<>();
//		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
//
//		when(currentLaunchConfig.getMaxSessions()).thenReturn(2);
//		when(localNode.getCurrentSessionCount()).thenReturn(2);
//
//		nodeActions.onNewSession(joinPoint);
//		verify(joinPoint, never()).proceed(new Object[] {createSessionRequest}); // check we do not continue the new session request
//	}
//
	@Test(groups={"grid"})
	public void testGetStatus() throws Throwable {
		when(currentLaunchConfig.getDevMode()).thenReturn(false);
		
		nodeActions.onGetStatus(joinPoint);
		verify(nodeActions).keepAlive();
	}
	
//	/**
//	 * Test when we set the node as inactive, Is supporting should reply false
//	 * @throws Throwable
//	 */
//	@Test(groups={"grid"})
//	public void testIsSupportingInactive() throws Throwable {
//		when(currentNodeConfig.getStatus()).thenReturn(GridStatus.INACTIVE);
//
//		Assert.assertFalse((boolean) nodeActions.onIsSupporting(joinPoint));
//		verify(joinPoint, never()).proceed(any());
//	}
//	/**
//	 * Test when we set the node as active, Is supporting should reply with the parent call
//	 * @throws Throwable
//	 */
//	@Test(groups={"grid"})
//	public void testIsSupportingActive() throws Throwable {
//		when(currentNodeConfig.getStatus()).thenReturn(GridStatus.ACTIVE);
//
//		nodeActions.onIsSupporting(joinPoint);
//		verify(joinPoint).proceed(any());
//	}
}
