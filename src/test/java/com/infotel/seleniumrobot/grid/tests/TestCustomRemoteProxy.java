package com.infotel.seleniumrobot.grid.tests;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.grid.internal.utils.GridHubConfiguration;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.JsonObject;
import com.infotel.seleniumrobot.grid.CustomRemoteProxy;
import com.infotel.seleniumrobot.grid.servlets.client.FileServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.utils.Utils;

@PrepareForTest({Utils.class})
public class TestCustomRemoteProxy extends BaseMockitoTest {

	
	GridHubConfiguration hubConfig = new GridHubConfiguration();
	
	@Mock
	Registry registry;
	
	@Mock
	CapabilityMatcher capabilityMatcher;
	
	@Mock
	FileServletClient fileServlet;
	
	@Mock
	NodeTaskServletClient nodeClient;
	
	RegistrationRequest request = RegistrationRequest.build("-role", "node");
	
	CustomRemoteProxy proxy;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		hubConfig.loadDefault();
		when(registry.getConfiguration()).thenReturn(hubConfig);
		when(registry.getCapabilityMatcher()).thenReturn(capabilityMatcher);
		when(capabilityMatcher.matches(anyObject(), anyObject())).thenReturn(true);
		PowerMockito.mockStatic(Utils.class);
		
		proxy = spy(new CustomRemoteProxy(request, registry, nodeClient, fileServlet));
	}
	
	/**
	 * By default, mocked matcher will answer true
	 */
	@Test(groups={"grid"})
	public void testHasCapabilities() {
		Map<String, Object> caps = new HashMap<>();
		Assert.assertTrue(proxy.hasCapability(caps));
	}
	
	/**
	 * Test that with 'doNotAcceptTestSessions' set to true, proxy prevent from sending sessions to this node
	 */
	@Test(groups={"grid"})
	public void testHasCapabilitiesWhenNotAcceptingSessions() {
		Map<String, Object> caps = new HashMap<>();
		proxy.setDoNotAcceptTestSessions(true);
		Assert.assertFalse(proxy.hasCapability(caps));
	}
	
	/**
	 * Standard case where node and hub versions are the same
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testIsAlive() throws ClientProtocolException, IOException, URISyntaxException {
		PowerMockito.when(Utils.getCurrentversion()).thenReturn("2.0.0");
		when(nodeClient.getVersion()).thenReturn("2.0.0");
		Mockito.doReturn(new JsonObject()).when(proxy).getStatus();
		
		Assert.assertTrue(proxy.isAlive());
	}
	
	/**
	 * Is alive when versions are not the same
	 * No upgrade because slots are still active
	 * @throws Exception 
	 */
	@Test(groups={"grid"})
	public void testIsAliveNoUpgradeWhenActive() throws Exception {
		PowerMockito.when(Utils.getCurrentversion()).thenReturn("2.0.0");
		when(nodeClient.getVersion()).thenReturn("1.0.0");
		Mockito.doReturn(new JsonObject()).when(proxy).getStatus();
		Mockito.doReturn(1).when(proxy).getTotalUsed();

		proxy.isAlive();
		
		verify(fileServlet, never()).upgrade(anyObject());
		Assert.assertTrue(proxy.isDoNotAcceptTestSessions());
	}
	
	/**
	 * Is alive when versions are not the same
	 * No upgrade because it has been already tried
	 * @throws Exception 
	 */
	@Test(groups={"grid"})
	public void testIsAliveNoUpgradeWhenAlreadyDone() throws Exception {
		PowerMockito.when(Utils.getCurrentversion()).thenReturn("2.0.0");
		when(nodeClient.getVersion()).thenReturn("1.0.0");
		Mockito.doReturn(new JsonObject()).when(proxy).getStatus();
		Mockito.doReturn(0).when(proxy).getTotalUsed();
		
		proxy.setUpgradeAttempted(true);
		File tempFile = File.createTempFile("grid", ".jar");
		tempFile.deleteOnExit();
		
		PowerMockito.when(Utils.getGridJar()).thenReturn(tempFile);
		
		proxy.isAlive();
		
		verify(fileServlet, never()).upgrade(anyObject());
		
		// check node still accept connections as it is not planned to be updated
		Assert.assertFalse(proxy.isDoNotAcceptTestSessions());
	}
	
	/**
	 * Is alive when versions are not the same
	 * No upgrade because we are in IDE mode, no jar available
	 * @throws Exception 
	 */
	@Test(groups={"grid"})
	public void testIsAliveNoUpgradeWhenIDEMode() throws Exception {
		PowerMockito.when(Utils.getCurrentversion()).thenReturn("2.0.0");
		when(nodeClient.getVersion()).thenReturn("1.0.0");
		Mockito.doReturn(new JsonObject()).when(proxy).getStatus();
		Mockito.doReturn(0).when(proxy).getTotalUsed();
		
		PowerMockito.when(Utils.getGridJar()).thenReturn(null);
		
		proxy.isAlive();
		
		verify(fileServlet, never()).upgrade(anyObject());
		Assert.assertFalse(proxy.isDoNotAcceptTestSessions());
		Assert.assertTrue(proxy.isUpgradeAttempted());
	}
	
	/**
	 * Is alive when versions are not the same
	 * Upgrade because we are in exec mode
	 * @throws Exception 
	 */
	@Test(groups={"grid"})
	public void testIsAliveUpgrade() throws Exception {
		PowerMockito.when(Utils.getCurrentversion()).thenReturn("2.0.0");
		when(nodeClient.getVersion()).thenReturn("1.0.0");
		Mockito.doReturn(new JsonObject()).when(proxy).getStatus();
		Mockito.doReturn(0).when(proxy).getTotalUsed();
		
		File tempFile = File.createTempFile("grid", ".jar");
		tempFile.deleteOnExit();
		
		PowerMockito.when(Utils.getGridJar()).thenReturn(tempFile);
		
		proxy.isAlive();
		
		verify(fileServlet, times(1)).upgrade(anyObject());
		
		// upgrade in progress, reset DoNotAcceptTestSessions flag
		Assert.assertFalse(proxy.isDoNotAcceptTestSessions());
		Assert.assertTrue(proxy.isUpgradeAttempted());
	}
	
}