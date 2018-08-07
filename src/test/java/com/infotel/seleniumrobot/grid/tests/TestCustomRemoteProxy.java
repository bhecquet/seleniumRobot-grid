package com.infotel.seleniumrobot.grid.tests;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.DefaultGridRegistry;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.grid.web.Hub;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.CustomRemoteProxy;
import com.infotel.seleniumrobot.grid.servlets.client.FileServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.seleniumtests.core.utils.SystemClock;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.util.helper.WaitHelper;

import io.appium.java_client.remote.MobileCapabilityType;

@PrepareForTest({Utils.class})
@PowerMockIgnore("javax.management.*")
public class TestCustomRemoteProxy extends BaseMockitoTest {

	
	GridHubConfiguration hubConfig = new GridHubConfiguration();
	GridNodeConfiguration nodeConfig = new GridNodeConfiguration();
	
	@Mock
	Hub hub;
	
	@Mock
	DefaultGridRegistry registry;
	
	@Mock
	CapabilityMatcher capabilityMatcher;
	
	@Mock
	FileServletClient fileServlet;
	
	@Mock
	NodeTaskServletClient nodeClient;
	
	@Mock
	HttpServletRequest servletRequest;
	
	@Mock
	TestSession testSession;
	
	@Mock
	TestSlot testSlot;
	
	@Mock
	HttpServletResponse servletResponse;
	
	@Mock
	MobileNodeServletClient mobileServletClient;
	
	RegistrationRequest request = RegistrationRequest.build(nodeConfig);
	
	CustomRemoteProxy proxy;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		when(registry.getHub()).thenReturn(hub);
		when(hub.getConfiguration()).thenReturn(hubConfig);
//		when(registry.getCapabilityMatcher()).thenReturn(capabilityMatcher);
//		when(capabilityMatcher.matches(anyObject(), anyObject())).thenReturn(true);
		PowerMockito.mockStatic(Utils.class);
		
		proxy = spy(new CustomRemoteProxy(request, registry, nodeClient, fileServlet, mobileServletClient, 5));
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
		Mockito.doReturn(new HashMap<>()).when(proxy).getProxyStatus();
		
		Assert.assertTrue(proxy.isAlive());
	}
	
	@Test(groups={"grid"})
	public void testIsAliveNoVersionGet() throws ClientProtocolException, IOException, URISyntaxException {
		PowerMockito.when(Utils.getCurrentversion()).thenReturn("2.0.0");
		when(nodeClient.getVersion()).thenReturn(null);
		Mockito.doReturn(new HashMap<>()).when(proxy).getProxyStatus();
		
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
		Mockito.doReturn(new HashMap<>()).when(proxy).getProxyStatus();
		Mockito.doReturn(1).when(proxy).getTotalUsed();

		proxy.isAlive();
		
		verify(fileServlet, never()).upgrade(anyString());
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
		Mockito.doReturn(new HashMap<>()).when(proxy).getProxyStatus();
		Mockito.doReturn(0).when(proxy).getTotalUsed();
		
		proxy.setUpgradeAttempted(true);
		File tempFile = File.createTempFile("grid", ".jar");
		tempFile.deleteOnExit();
		
		PowerMockito.when(Utils.getGridJar()).thenReturn(tempFile);
		
		proxy.isAlive();
		
		verify(fileServlet, never()).upgrade(anyString());
		
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
		Mockito.doReturn(new HashMap<>()).when(proxy).getProxyStatus();
		Mockito.doReturn(0).when(proxy).getTotalUsed();
		
		PowerMockito.when(Utils.getGridJar()).thenReturn(null);
		
		proxy.isAlive();
		
		verify(fileServlet, never()).upgrade(anyString());
		Assert.assertFalse(proxy.isDoNotAcceptTestSessions());
		Assert.assertTrue(proxy.isUpgradeAttempted());
	}
	
	/**
	 * Is alive when versions are not the same
	 * Upgrade because we are in exec mode
	 * 
	 * TEST IS DISABLED because auto upgrade is not active
	 * @throws Exception 
	 */
	@Test(groups={"grid"}, enabled=false)
	public void testIsAliveUpgrade() throws Exception {
		PowerMockito.when(Utils.getCurrentversion()).thenReturn("2.0.0");
		when(nodeClient.getVersion()).thenReturn("1.0.0");
		Mockito.doReturn(new HashMap<>()).when(proxy).getProxyStatus();
		Mockito.doReturn(0).when(proxy).getTotalUsed();
		
		File tempFile = File.createTempFile("grid", ".jar");
		tempFile.deleteOnExit();
		
		PowerMockito.when(Utils.getGridJar()).thenReturn(tempFile);
		
		proxy.isAlive();
		
		verify(fileServlet, times(1)).upgrade(anyString());
		
		// upgrade in progress, reset DoNotAcceptTestSessions flag
		Assert.assertFalse(proxy.isDoNotAcceptTestSessions());
		Assert.assertTrue(proxy.isUpgradeAttempted());
	}
	
	@Test(groups={"grid"})
	public void testBeforeSessionDoNotChangeStandardCaps() {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(caps);
		proxy.beforeSession(testSession);
		Assert.assertEquals(caps.get("key"), "value");
	}
	
	/**
	 * check that file path is updated with a remote URL
	 */
	@Test(groups={"grid"})
	public void testBeforeSessionChangeUploadedFilePath() {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "file:aFolder/aFile");
		
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(caps);
		proxy.beforeSession(testSession);
		Assert.assertEquals(caps.get("key"), "http://localhost:4444/grid/admin/FileServlet/aFolder/aFile");
	}
	
	/**
	 * Test that when 'platformName' is defined, we call mobileServlet to update capabilities with node caps
	 * It allows to switch from a human readable name to an ID on android
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testBeforeSessionUpdateMobileDeviceName() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		caps.put("deviceName", "device1");
		caps.put(MobileCapabilityType.PLATFORM_NAME, "ios");
		
		DesiredCapabilities newCaps = new DesiredCapabilities(caps);
		newCaps.setCapability("deviceName", "id-1234");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(caps))).thenReturn(newCaps);
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(caps);
		proxy.beforeSession(testSession);
		Assert.assertEquals(caps.get("key"), "value");
		Assert.assertEquals(caps.get("deviceName"), "id-1234");
	}	
	
	/**
	 * Test that chrome driver path is added to session capabilities
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testChromeDriverAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCaps.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver1.exe");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertEquals(testSession.getRequestedCapabilities().get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY), "chromedriver1.exe");
	}
	
	/**
	 * Test that gecko driver path is added to session capabilities
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testFirefoxDriverAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "firefox");
		requestedCaps.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, "geckodriver1.exe");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertEquals(testSession.getRequestedCapabilities().get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY), "geckodriver1.exe");
	}
	
	/**
	 * Test that IE driver path is added to session capabilities
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testIeDriverAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "internet explorer");
		requestedCaps.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, "iedriver1.exe");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertEquals(testSession.getRequestedCapabilities().get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY), "iedriver1.exe");
	}
	
	/**
	 * Test that edge driver path is added to session capabilities
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testEdgeDriverAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, BrowserType.EDGE);
		requestedCaps.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY, "edgedriver1.exe");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertEquals(testSession.getRequestedCapabilities().get(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY), "edgedriver1.exe");
	}
	
	/**
	 * Test that no driver is added if browsername not specified
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testNoDriverAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY, "edgedriver1.exe");
		nodeCaps.put(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, "iedriver1.exe");
		nodeCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, "geckodriver1.exe");
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver1.exe");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertNull(testSession.getRequestedCapabilities().get(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY));
		Assert.assertNull(testSession.getRequestedCapabilities().get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY));
		Assert.assertNull(testSession.getRequestedCapabilities().get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY));
		Assert.assertNull(testSession.getRequestedCapabilities().get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY));
	}
	
	/**
	 * Prepare data for beforeCommand tests
	 * @param requestBody
	 * @param nodeCaps
	 * @throws IOException 
	 */
	private void prepareServletRequest(String requestBody, Map<String, Object> nodeCaps, String requestPath, String method) throws IOException {
		InputStream byteArrayInputStream = IOUtils.toInputStream(new JSONObject(requestBody).toString(), Charset.forName("UTF-8"));
		ServletInputStream mockServletInputStream = mock(ServletInputStream.class);

		Answer<Integer> inputStreamReadAnswer = new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
				Object[] args = invocationOnMock.getArguments();
				return byteArrayInputStream.read((byte[]) args[0]);
			}
		};
		
		Answer<Integer> inputStreamReadAnswer2 = new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
				Object[] args = invocationOnMock.getArguments();
				return byteArrayInputStream.read((byte[]) args[0], (int) args[1], (int) args[2]);
			}
		};
		
		when(mockServletInputStream.read(ArgumentMatchers.<byte[]>any())).thenAnswer(inputStreamReadAnswer);
		when(mockServletInputStream.read(ArgumentMatchers.<byte[]>any(), anyInt(), anyInt())).thenAnswer(inputStreamReadAnswer2);
		
		when(servletRequest.getServletPath()).thenReturn("/wd/hub");
		when(servletRequest.getMethod()).thenReturn(method);
		when(servletRequest.getPathInfo()).thenReturn(requestPath);
		when(servletRequest.getInputStream()).thenReturn(mockServletInputStream);
		when(testSession.getRequestedCapabilities()).thenReturn(nodeCaps);
		
	}
	
	/**
	 * Test that forwarded new session request is updated with node capabilities before being sent to node
	 * This allows to precise some information that are known by node and not by requester
	 * e.g: UDID or ID of virtual mobile devices, driver path, ...
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testBeforeCommandCapsUpdated() throws ClientProtocolException, IOException, URISyntaxException {

		String requestBody = "{"
								+ "\"capabilities\": {"
								+ "    \"desiredCapabilities\": {"
								+ "         \"platformName\":\"android\","
								+ "         \"browser\":\"chrome\""
								+ "    }"
								+ "},"
								+ "\"desiredCapabilities\": {"
								+ "     \"platformName\":\"android\","
								+ "     \"browser\":\"chrome\""
								+ "},"
								+ "\"firstMatch\": {"
								+ "    \"browser\":\"chrome\""
								+ "}"
							+ "}";
		
		Map<String, Object> nodeCaps = new HashMap<>();
		nodeCaps.put(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCaps.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver.exe");
		
		prepareServletRequest(requestBody, nodeCaps, "/session", "POST");
				
		HttpServletRequest req = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		
		proxy.beforeCommand(testSession, req, servletResponse);
		
		// check request body changed
		String body = ((SeleniumBasedRequest)req).getBody();
		JSONObject map = new JSONObject(body);
		
		// check structure has not been modified
		Assert.assertTrue(map.has("capabilities"));
		Assert.assertTrue(map.getJSONObject("capabilities").has("desiredCapabilities"));
		Assert.assertTrue(map.has("desiredCapabilities"));
		Assert.assertTrue(map.has("firstMatch"));
		
		// check new value replaced old
		Assert.assertEquals(map.getJSONObject("capabilities").getJSONObject("desiredCapabilities").get(MobileCapabilityType.PLATFORM_NAME), "android");
		Assert.assertEquals(map.getJSONObject("desiredCapabilities").get(MobileCapabilityType.PLATFORM_NAME), "android");
		Assert.assertEquals(map.getJSONObject("capabilities").getJSONObject("desiredCapabilities").get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY), "chromedriver.exe");
		Assert.assertEquals(map.getJSONObject("desiredCapabilities").get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY), "chromedriver.exe");
	}
	
	/**
	 * Test that request is not changed when capabilities structure is not found
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testBeforeCommandNoCaps() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		
		String requestBody = "{"
				+ "\"timeout\": 120"
				+ "}";
		
		Map<String, Object> nodeCaps = new HashMap<>();
		nodeCaps.put(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCaps.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver.exe");
		
		prepareServletRequest(requestBody, nodeCaps, "/command", "POST");
		
		HttpServletRequest req = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		
		proxy.beforeCommand(testSession, req, servletResponse);
		
		// check request body changed
		String body = ((SeleniumBasedRequest)req).getBody();
		JSONObject map = new JSONObject(body);
		
		// check request body has not been modified
		Assert.assertEquals(map.getInt("timeout"), 120);
		Assert.assertFalse(map.has("desiredCapabilities"));
		Assert.assertFalse(map.has("capabilities"));
		
	}
	
	/**
	 * Test that beforeCommand & afterCommand correctly handle 'startSession' and 'stopSession' to read list of pids for our driver and kill browser at the end
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void browserPidKilling() throws ClientProtocolException, IOException, URISyntaxException, UnirestException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		
		String startSessionRequestBody = "{\r\n" + 
				"  \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"  \"capabilities\": {\r\n" + 
				"    \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"    \"firstMatch\": [\r\n" + 
				"      {\"browserName\":\"chrome\",\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\"}\r\n" + 
				"    ]\r\n" + 
				"  }\r\n" + 
				"}";
		
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(new ArrayList<>()))).thenReturn(Arrays.asList(1000L)); // for initial search
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(1000L)))).thenReturn(Arrays.asList(2000L)); // after driver creation
		when(nodeClient.getBrowserAndDriverPids(anyString(), anyString(), eq(Arrays.asList(2000L)))).thenReturn(Arrays.asList(2000L, 2010L, 2020L)); // driver + browser pids
		
		prepareServletRequest(startSessionRequestBody, new HashMap<>(), "/session", "POST");
		HttpServletRequest reqStart = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		proxy.beforeCommand(testSession, reqStart, servletResponse);
		Assert.assertEquals(testSession.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(1000L));
		
		proxy.afterCommand(testSession, reqStart, servletResponse);
		Assert.assertEquals(testSession.get(CustomRemoteProxy.CURRENT_DRIVER_PIDS), Arrays.asList(2000L));
		
		prepareServletRequest(startSessionRequestBody, new HashMap<>(), "/session/340c2e79e402ce6e6396df4d8140282a", "DELETE");
		HttpServletRequest reqStop = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		proxy.beforeCommand(testSession, reqStop, servletResponse);
		Assert.assertEquals(testSession.get(CustomRemoteProxy.PIDS_TO_KILL), Arrays.asList(2000L, 2010L, 2020L));
		verify(nodeClient, never()).killProcessByPid(2000L);
		
		proxy.afterCommand(testSession, reqStop, servletResponse);
		
		// check pids are killed at the end of the session
		verify(nodeClient).killProcessByPid(2000L);
		verify(nodeClient).killProcessByPid(2010L);
		verify(nodeClient).killProcessByPid(2020L);
	}
	
	/**
	 * Test that if several thread create a session on the same node, the second thread waits for first thread action terminating (session creation) before going on
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 * @throws InterruptedException 
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingSession() throws ClientProtocolException, IOException, URISyntaxException, UnirestException, InterruptedException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession1 = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		TestSession testSession2 = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		
		String startSessionRequestBody = "{\r\n" + 
				"  \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"  \"capabilities\": {\r\n" + 
				"    \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"    \"firstMatch\": [\r\n" + 
				"      {\"browserName\":\"chrome\",\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\"}\r\n" + 
				"    ]\r\n" + 
				"  }\r\n" + 
				"}";
		
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(new ArrayList<>()))).thenReturn(Arrays.asList(1000L), Arrays.asList(2000L)); // for initial search
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(1000L)))).thenReturn(Arrays.asList(3000L)); // after driver creation
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(2000L)))).thenReturn(Arrays.asList(4000L)); // after driver creation
		
		prepareServletRequest(startSessionRequestBody, new HashMap<>(), "/session", "POST");
		HttpServletRequest reqStart = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		
		SystemClock clock = new SystemClock();
		long start = clock.now();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());


		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			proxy.beforeCommand(testSession1, reqStart, servletResponse);
			ends.put(1, clock.now() - start);
			Assert.assertEquals(testSession1.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(1000L));
			Assert.assertNull(testSession2.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS));
			WaitHelper.waitForSeconds(2);
			proxy.afterCommand(testSession1, reqStart, servletResponse);
	      });
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			proxy.beforeCommand(testSession2, reqStart, servletResponse);
			ends.put(2, clock.now() - start);
			Assert.assertEquals(testSession1.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(1000L));
			Assert.assertEquals(testSession2.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(2000L));
		});
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		
		// check that thread 2 blocked for at least 2 seconds (meaning it started after 'afterCommand' call of thread 1)
		Assert.assertTrue(ends.get(2) > 2000);
		
	}
	
	/**
	 * Test that if several thread create a session on the same node, the second thread waits for first thread action terminating (session creation) before going on
	 * Here, we check that if 'afterCommand' (which unlocks lock) is not called, lock is unlocked after the defined amount of time (5 secs for tests)
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 * @throws InterruptedException 
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingSessionAfterCommandNotCalled() throws ClientProtocolException, IOException, URISyntaxException, UnirestException, InterruptedException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession1 = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		TestSession testSession2 = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		
		String startSessionRequestBody = "{\r\n" + 
				"  \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"  \"capabilities\": {\r\n" + 
				"    \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"    \"firstMatch\": [\r\n" + 
				"      {\"browserName\":\"chrome\",\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\"}\r\n" + 
				"    ]\r\n" + 
				"  }\r\n" + 
				"}";
		
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(new ArrayList<>()))).thenReturn(Arrays.asList(1000L), Arrays.asList(2000L)); // for initial search
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(1000L)))).thenReturn(Arrays.asList(3000L)); // after driver creation
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(2000L)))).thenReturn(Arrays.asList(4000L)); // after driver creation
		
		prepareServletRequest(startSessionRequestBody, new HashMap<>(), "/session", "POST");
		HttpServletRequest reqStart = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		
		SystemClock clock = new SystemClock();
		long start = clock.now();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());
		
		
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		
		// do not call 'afterCommand' to simulate the case where error could occur in forward method (caller)
		executorService.submit(() -> {
			proxy.beforeCommand(testSession1, reqStart, servletResponse);
			ends.put(1, clock.now() - start);
			Assert.assertEquals(testSession1.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(1000L));
			Assert.assertNull(testSession2.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS));
		});
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			proxy.beforeCommand(testSession2, reqStart, servletResponse);
			ends.put(2, clock.now() - start);
			Assert.assertEquals(testSession1.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(1000L));
			Assert.assertEquals(testSession2.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(2000L));
		});
		executorService.shutdown();
		executorService.awaitTermination(15, TimeUnit.SECONDS);
		System.out.println(ends);
		
		// check that thread 2 blocked for at least 2 seconds (meaning it started after 'afterCommand' call of thread 1)
		Assert.assertTrue(ends.get(2) > 5000);
		Assert.assertTrue(ends.get(2) < 6000);
		
	}
	
	/**
	 * Test that if several thread create a session on the same node, the second thread waits for first thread action terminating (session creation) before going on
	 * Here, first thread throws an error, so afterSession will never be called but unlock should be done
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 * @throws InterruptedException 
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingSessionWithException() throws ClientProtocolException, IOException, URISyntaxException, UnirestException, InterruptedException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession1 = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		TestSession testSession2 = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		
		String startSessionRequestBody = "{\r\n" + 
				"  \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"  \"capabilities\": {\r\n" + 
				"    \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"    \"firstMatch\": [\r\n" + 
				"      {\"browserName\":\"chrome\",\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\"}\r\n" + 
				"    ]\r\n" + 
				"  }\r\n" + 
				"}";
		
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(new ArrayList<>()))).thenThrow(new ConfigurationException("some exception")).thenReturn(Arrays.asList(2000L)); // for initial search
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(1000L)))).thenReturn(Arrays.asList(3000L)); // after driver creation
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(2000L)))).thenReturn(Arrays.asList(4000L)); // after driver creation
		
		prepareServletRequest(startSessionRequestBody, new HashMap<>(), "/session", "POST");
		HttpServletRequest reqStart = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		
		SystemClock clock = new SystemClock();
		long start = clock.now();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());
		
		
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			proxy.beforeCommand(testSession1, reqStart, servletResponse);
		});
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			proxy.beforeCommand(testSession2, reqStart, servletResponse);
			ends.put(2, clock.now() - start);
			Assert.assertNull(testSession1.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS));
			Assert.assertEquals(testSession2.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(2000L));
		});
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		
		// check that thread 2 until lock timeout (5 secs for unit tests)
		Assert.assertTrue(ends.get(2) < 1000);
		
	}
	
	/**
	 * when starting session, if no pre-existing pid has been returned, we get a new ArrayList()
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void browserBeforeTestSession() throws ClientProtocolException, IOException, URISyntaxException, UnirestException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		
		String startSessionRequestBody = "{\r\n" + 
				"  \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"  \"capabilities\": {\r\n" + 
				"    \"desiredCapabilities\": {\"acceptSslCerts\":true,\"browserName\":\"chrome\",\"javascriptEnabled\":true,\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\",\"takesScreenshot\":true,\"webdriver.chrome.driver\":\"D:/Dev/seleniumRobot-grid/drivers/chromedriver_2.38_chrome-65-67.exe\"},\r\n" + 
				"    \"firstMatch\": [\r\n" + 
				"      {\"browserName\":\"chrome\",\"proxy\":{\"proxyType\":\"direct\"},\"se:CONFIG_UUID\":\"181963b8-c041-4a5e-9c2c-e57b089d5f2e\"}\r\n" + 
				"    ]\r\n" + 
				"  }\r\n" + 
				"}";
		
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(new ArrayList<>()))).thenReturn(Arrays.asList(1000L)); // for initial search
		when(nodeClient.getDriverPids(anyString(), anyString(), eq(Arrays.asList(1000L)))).thenReturn(Arrays.asList(2000L)); // after driver creation

		prepareServletRequest(startSessionRequestBody, new HashMap<>(), "/session", "POST");
		HttpServletRequest reqStart = SeleniumBasedRequest.createFromRequest(servletRequest, registry);
		testSession.put(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS, null);
		
		proxy.afterCommand(testSession, reqStart, servletResponse);
		Assert.assertEquals(testSession.get(CustomRemoteProxy.CURRENT_DRIVER_PIDS), new ArrayList<>());
	}
	
	/**
	 * Test that session finalization stop video capture, appium and kill processes
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void browserFinalizeSession() throws ClientProtocolException, IOException, URISyntaxException, UnirestException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		testSession.setExternalKey(new ExternalSessionKey("340c2e79e402ce6e6396df4d8140282a"));
		testSession.put(CustomRemoteProxy.PIDS_TO_KILL, Arrays.asList(2000L, 2010L));
		
		proxy.afterSession(testSession);
		verify(nodeClient).stopVideoCapture(testSession.getExternalKey().getKey());
		verify(nodeClient).stopAppium(testSession.getInternalKey());
		verify(nodeClient).killProcessByPid(2000L);
		verify(nodeClient).killProcessByPid(2010L);
	}
	
	/**
	 * Test that session finalization when driver has not been create (external session is null). No error should be raised
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void browserFinalizeSessionWithoutExternalSession() throws ClientProtocolException, IOException, URISyntaxException, UnirestException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession = new TestSession(testSlot, requestedCapabilities, Clock.systemUTC());
		testSession.put(CustomRemoteProxy.PIDS_TO_KILL, Arrays.asList(2000L, 2010L));
		
		proxy.afterSession(testSession);
		verify(nodeClient, never()).stopVideoCapture(anyString());
		verify(nodeClient).stopAppium(testSession.getInternalKey());
		verify(nodeClient).killProcessByPid(2000L);
		verify(nodeClient).killProcessByPid(2010L);
	}
}
