package com.infotel.seleniumrobot.grid.tests;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.grid.web.Hub;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
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
import com.infotel.seleniumrobot.grid.CustomRemoteProxyWrapper;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.StatusServlet;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.util.helper.WaitHelper;

import io.appium.java_client.remote.MobileCapabilityType;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

@PrepareForTest({Utils.class, LaunchConfig.class})
@PowerMockIgnore("javax.management.*")
public class TestCustomRemoteProxy extends BaseMockitoTest {

	
	GridHubConfiguration hubConfig = new GridHubConfiguration();
	GridNodeConfiguration nodeConfig = new GridNodeConfiguration();
	
	@Mock
	Hub hub;
	
	@Mock
	GridRegistry registry;
	
	@Mock
	LaunchConfig launchConfig;
	
	@Mock
	CapabilityMatcher capabilityMatcher;
	
	@Mock
	NodeTaskServletClient nodeClient;
	
	@Mock
	NodeStatusServletClient nodeStatusClient;

	@Mock
	MobileNodeServletClient mobileServletClient;
	
	@Mock
	HttpServletRequest servletRequest;
	
	@Mock
	TestSession testSession;
	
	@Mock
	TestSlot testSlot1;
	
	@Mock
	TestSlot testSlot2;
	
	@Mock
	HttpServletResponse servletResponse;
	
	
	RegistrationRequest request = RegistrationRequest.build(nodeConfig);
	JSONObject nodeStatus;
	
	CustomRemoteProxy proxy;
	
	@BeforeMethod(groups={"grid"})
	public void setup() throws Exception {
		when(registry.getHub()).thenReturn(hub);
		when(hub.getConfiguration()).thenReturn(hubConfig);
//		when(registry.getCapabilityMatcher()).thenReturn(capabilityMatcher);
//		when(capabilityMatcher.matches(anyObject(), anyObject())).thenReturn(true);
		PowerMockito.mockStatic(Utils.class);
		PowerMockito.mockStatic(LaunchConfig.class);
		
		proxy = spy(new CustomRemoteProxy(request, registry, nodeClient, nodeStatusClient, mobileServletClient, 5));
		proxy.setHubStatus(GridStatus.ACTIVE);
		
		nodeStatus = new JSONObject("{\r\n" + 
					"  \"memory\": {\r\n" + 
					"    \"totalMemory\": 17054,\r\n" + 
					"    \"class\": \"com.infotel.seleniumrobot.grid.utils.MemoryInfo\",\r\n" + 
					"    \"freeMemory\": 5035\r\n" + 
					"  },\r\n" + 
					"  \"maxSessions\": 1,\r\n" + 
					"  \"port\": 5554,\r\n" + 
					"  \"ip\": \"SN782980\",\r\n" + 
					"  \"cpu\": 0.0,\r\n" + 
					"  \"version\": \"3.14.0-SNAPSHOT\",\r\n" + 
					"  \"status\": \"ACTIVE\"\r\n" + 
					"}");
		

		when(nodeStatusClient.getStatus()).thenReturn(nodeStatus);
		PowerMockito.when(LaunchConfig.class, "getCurrentLaunchConfig").thenReturn(launchConfig);
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
	 * Test that with status set to inactive, proxy prevent from sending sessions to this node
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testHasCapabilitiesWhenNodeNotAcceptingSessions() throws UnirestException {
		Map<String, Object> caps = new HashMap<>();
		nodeStatus.put("status", GridStatus.INACTIVE.toString());
		proxy.getRegistry().getHub().getConfiguration().custom.put(StatusServlet.STATUS, GridStatus.ACTIVE.toString());
		Assert.assertFalse(proxy.hasCapability(caps));
	}
	
	/**
	 * Test that with status set to active, proxy sends sessions to this node
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testHasCapabilitiesWhenNodeAcceptingSessions() throws UnirestException {
		Map<String, Object> caps = new HashMap<>();
		nodeStatus.put("status", GridStatus.ACTIVE.toString());
		proxy.getRegistry().getHub().getConfiguration().custom.put(StatusServlet.STATUS, GridStatus.ACTIVE.toString());
		Assert.assertTrue(proxy.hasCapability(caps));
	}
	
	/**
	 * If hub is set inactive no session will be created on any node even if they are active
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testHasCapabilitiesWhenHubNotAcceptingSessions() throws UnirestException {
		Map<String, Object> caps = new HashMap<>();
		nodeStatus.put("status", GridStatus.ACTIVE.toString());
		proxy.getRegistry().getHub().getConfiguration().custom.put(StatusServlet.STATUS, GridStatus.INACTIVE.toString());
		Assert.assertFalse(proxy.hasCapability(caps));
	}
	
	/**
	 * If hub is set active, session will be created on any active node
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testHasCapabilitiesWhenHubAcceptingSessions() throws UnirestException {
		Map<String, Object> caps = new HashMap<>();
		nodeStatus.put("status", GridStatus.ACTIVE.toString());
		proxy.getRegistry().getHub().getConfiguration().custom.put(StatusServlet.STATUS, GridStatus.ACTIVE.toString());
		Assert.assertTrue(proxy.hasCapability(caps));
	}
	
	/**
	 * If hub is set to null active mode (the default), session will be created on any active node
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testHasCapabilitiesWhenHubActiveModeSetToNul() throws UnirestException {
		Map<String, Object> caps = new HashMap<>();
		nodeStatus.put("status", GridStatus.ACTIVE.toString());
		proxy.getRegistry().getHub().getConfiguration().custom.put(StatusServlet.STATUS, GridStatus.ACTIVE.toString());
		Assert.assertTrue(proxy.hasCapability(caps));
	}
	
	/**
	 * BeforeSession method should not change capabilities provided by session request, only add new ones
	 */
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
	 * issue #54: Test that when 'platform' is defined with precise OS, for desktop tests, we change platform and platformName capabilities 
	 * Here, Windows 7 (Vista) => Windows
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testBeforeSessionUpdatePlatformWindow7Caps() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> caps = new HashMap<>();
		caps.put(CapabilityType.PLATFORM_NAME, "VISTA");
		caps.put(CapabilityType.PLATFORM, Platform.VISTA);
		
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(caps);
		proxy.beforeSession(testSession);
		Assert.assertEquals(caps.get(CapabilityType.PLATFORM_NAME), Platform.WINDOWS.toString());
		Assert.assertEquals(caps.get(CapabilityType.PLATFORM), Platform.WINDOWS);
	}	
	
	/**
	 * issue #54: Test that when 'platform' is defined with general OS, for desktop tests, we do not change platform and platformName capabilities 
	 * Here, Windows  => Windows
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testBeforeSessionUpdatePlatformWindowCaps() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> caps = new HashMap<>();
		caps.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		caps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(caps);
		proxy.beforeSession(testSession);
		Assert.assertEquals(caps.get(CapabilityType.PLATFORM_NAME), Platform.WINDOWS.toString());
		Assert.assertEquals(caps.get(CapabilityType.PLATFORM), Platform.WINDOWS);
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
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver1.exe");
		nodeCaps.put("chrome_binary", "/home/chrome");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertEquals(testSession.getRequestedCapabilities().get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY), "chromedriver1.exe");
		
		// issue #60: check binary is also there
		Assert.assertEquals(((Map<String, Object>)testSession.getRequestedCapabilities().get(ChromeOptions.CAPABILITY)).get("binary"), "/home/chrome");
	}
	
	@Test(groups={"grid"})
	public void testChromeDefaultProfileAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
		requestedCaps.put("chromeProfile", "default");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver1.exe");
		nodeCaps.put("chrome_binary", "/home/chrome");
		nodeCaps.put("defaultProfilePath", "/home/chrome/profile");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertTrue(((Map<String, List<String>>)testSession.getRequestedCapabilities().get(ChromeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/chrome/profile"));
	}
	
	@Test(groups={"grid"})
	public void testChromeUserProfileAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
		requestedCaps.put("chromeProfile", "/home/user/myprofile");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver1.exe");
		nodeCaps.put("chrome_binary", "/home/chrome");
		nodeCaps.put("defaultProfilePath", "/home/chrome/profile");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertTrue(((Map<String, List<String>>)testSession.getRequestedCapabilities().get(ChromeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/user/myprofile"));
	}
	
	@Test(groups={"grid"})
	public void testChromeNoUserProfileAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver1.exe");
		nodeCaps.put("chrome_binary", "/home/chrome");
		nodeCaps.put("defaultProfilePath", "/home/chrome/profile");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertEquals(((Map<String, List<String>>)testSession.getRequestedCapabilities().get(ChromeOptions.CAPABILITY)).get("args").size(), 0);
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
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, "geckodriver1.exe");
		nodeCaps.put(FirefoxDriver.BINARY, "/home/firefox");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		Assert.assertEquals(testSession.getRequestedCapabilities().get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY), "geckodriver1.exe");
		
		// issue #60: check binary is also there
		Assert.assertEquals(testSession.getRequestedCapabilities().get(FirefoxDriver.BINARY), "/home/firefox");
	}
	

	/**
	 * Check profile has been updated ('firefoxProfile' cap is  set to default). Some initial preferences are kept 'general.useragent.override' and 'network.automatic-ntlm-auth.trusted-uris'
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testFirefoxDefaultProfileAdded() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "firefox");
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		requestedCaps.put(FirefoxDriver.PROFILE, new FirefoxProfile().toJson());
		requestedCaps.put("firefoxProfile", "default");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, "geckodriver1.exe");
		nodeCaps.put(FirefoxDriver.BINARY, "/home/firefox");
		nodeCaps.put("defaultProfilePath", "/home/firefox/profile");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		FirefoxProfile newProfile = FirefoxProfile.fromJson((String) testSession.getRequestedCapabilities().get(FirefoxDriver.PROFILE));
		
		// check default preferences
		Assert.assertEquals(newProfile.getStringPreference("general.useragent.override", "no"), "no");
		Assert.assertEquals(newProfile.getStringPreference("network.automatic-ntlm-auth.trusted-uris", "no"), "no");
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Window.QueryInterface", ""), CustomRemoteProxy.ALL_ACCESS);
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Window.frameElement.get", ""),  CustomRemoteProxy.ALL_ACCESS);
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.HTMLDocument.compatMode.get", ""),  CustomRemoteProxy.ALL_ACCESS);
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Document.compatMode.get", ""),  CustomRemoteProxy.ALL_ACCESS);
		Assert.assertEquals(newProfile.getIntegerPreference("dom.max_chrome_script_run_time", 10), 0);
        Assert.assertEquals(newProfile.getIntegerPreference("dom.max_script_run_time", 10), 0);
	}
	
	@Test(groups={"grid"})
	public void testFirefoxDefaultProfileAdded2() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "firefox");
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("general.useragent.override", "ua");
		profile.setPreference("network.automatic-ntlm-auth.trusted-uris", "uri");
		profile.setPreference("mypref", "mp");
		requestedCaps.put(FirefoxDriver.PROFILE, profile.toJson());
		requestedCaps.put("firefoxProfile", "default");
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, "geckodriver1.exe");
		nodeCaps.put(FirefoxDriver.BINARY, "/home/firefox");
		nodeCaps.put("defaultProfilePath", "/home/firefox/profile");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		FirefoxProfile newProfile = FirefoxProfile.fromJson((String) testSession.getRequestedCapabilities().get(FirefoxDriver.PROFILE));
		
		// check updated preferences

		Assert.assertEquals(newProfile.getStringPreference("mypref", "no"), "no"); // this property is not handled, so not kept
		Assert.assertEquals(newProfile.getStringPreference("general.useragent.override", "no"), "ua");
		Assert.assertEquals(newProfile.getStringPreference("network.automatic-ntlm-auth.trusted-uris", "no"), "uri");
	
	}
	
	/**
	 * Check profile has not been updated ('firefoxProfile' cap is not set)
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testFirefoxNoDefaultProfile() throws ClientProtocolException, IOException, URISyntaxException {
		TestSession testSession = Mockito.mock(TestSession.class);
		TestSlot testSlot = Mockito.mock(TestSlot.class);
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, "firefox");
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("mypref", "mp");
		requestedCaps.put(FirefoxDriver.PROFILE, profile.toJson());
		
		Map<String, Object> nodeCaps = new HashMap<>(requestedCaps);
		nodeCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, "geckodriver1.exe");
		nodeCaps.put(FirefoxDriver.BINARY, "/home/firefox");
		nodeCaps.put("defaultProfilePath", "/home/firefox/profile");
		
		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps))).thenReturn(new DesiredCapabilities(requestedCaps));
		when(testSession.getSlot()).thenReturn(testSlot);
		when(testSession.getRequestedCapabilities()).thenReturn(requestedCaps);
		when(testSlot.getCapabilities()).thenReturn(nodeCaps);
		proxy.beforeSession(testSession);
		
		FirefoxProfile newProfile = FirefoxProfile.fromJson((String) testSession.getRequestedCapabilities().get(FirefoxDriver.PROFILE));
		
		// check updated preferences
		Assert.assertEquals(newProfile.getStringPreference("mypref", "no"), "mp");
		
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
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		
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
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		
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
		requestedCaps.put(CapabilityType.PLATFORM, Platform.WINDOWS);
		
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
		InputStream byteArrayInputStream = IOUtils.toInputStream(new JSONObject(requestBody).toString(), StandardCharsets.UTF_8);
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
		TestSession testSession = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		
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
		TestSession testSession1 = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		TestSession testSession2 = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		
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
		
		Clock clock = Clock.systemUTC();
		Instant start = clock.instant();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());


		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			proxy.beforeCommand(testSession1, reqStart, servletResponse);
			ends.put(1, clock.instant().toEpochMilli() - start.toEpochMilli());
			Assert.assertEquals(testSession1.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(1000L));
			Assert.assertNull(testSession2.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS));
			WaitHelper.waitForSeconds(2);
			proxy.afterCommand(testSession1, reqStart, servletResponse);
	      });
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			proxy.beforeCommand(testSession2, reqStart, servletResponse);
			ends.put(2, clock.instant().toEpochMilli() - start.toEpochMilli());
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
		TestSession testSession1 = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		TestSession testSession2 = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		
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
		
		Clock clock = Clock.systemUTC();
		Instant start = clock.instant();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());
		
		
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		
		// do not call 'afterCommand' to simulate the case where error could occur in forward method (caller)
		executorService.submit(() -> {
			proxy.beforeCommand(testSession1, reqStart, servletResponse);
			ends.put(1, clock.instant().toEpochMilli() - start.toEpochMilli());
			Assert.assertEquals(testSession1.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS), Arrays.asList(1000L));
			Assert.assertNull(testSession2.get(CustomRemoteProxy.PREEXISTING_DRIVER_PIDS));
		});
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			proxy.beforeCommand(testSession2, reqStart, servletResponse);
			ends.put(2, clock.instant().toEpochMilli() - start.toEpochMilli());
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
		TestSession testSession1 = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		TestSession testSession2 = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		
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
		
		Clock clock = Clock.systemUTC();
		Instant start = clock.instant();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());
		
		
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			proxy.beforeCommand(testSession1, reqStart, servletResponse);
		});
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			proxy.beforeCommand(testSession2, reqStart, servletResponse);
			ends.put(2, clock.instant().toEpochMilli() - start.toEpochMilli());
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
		TestSession testSession = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		
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
		TestSession testSession = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
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
		TestSession testSession = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		testSession.put(CustomRemoteProxy.PIDS_TO_KILL, Arrays.asList(2000L, 2010L));
		
		proxy.afterSession(testSession);
		verify(nodeClient, never()).stopVideoCapture(anyString());
		verify(nodeClient).stopAppium(testSession.getInternalKey());
		verify(nodeClient).killProcessByPid(2000L);
		verify(nodeClient).killProcessByPid(2010L);
	}
	
	/**
	 * Max node session set and reached, check node is disabled
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDisableNodeIfMaxSessionsReached() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(10);
		
		proxy.disableNodeIfMaxSessionsReached();
		
		verify(nodeStatusClient).setStatus(GridStatus.INACTIVE);
	}
	
	/**
	 * Max node session set and not reached, check node is not disabled
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDoNotDisableNodeIfMaxSessionsNotReached() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(9);
		
		proxy.disableNodeIfMaxSessionsReached();
		
		verify(nodeStatusClient, never()).setStatus(GridStatus.INACTIVE);
	}
	
	/**
	 * Max node session set but valued to 0, check node is not disabled
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDoNotDisableNodeIfMaxSessionsIs0() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(0);
		proxy.setTestSessionsCount(9);
		
		proxy.disableNodeIfMaxSessionsReached();
		
		verify(nodeStatusClient, never()).setStatus(GridStatus.INACTIVE);
	}
	
	/**
	 * Max node session not set, check node is not disabled
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDoNotDisableNodeIfMaxSessionsNotSet() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(null);
		proxy.setTestSessionsCount(100);
		
		proxy.disableNodeIfMaxSessionsReached();
		
		verify(nodeStatusClient, never()).setStatus(GridStatus.INACTIVE);
	}
	
	/**
	 * Max hub session set and reached
	 * less than 10% of test slots are in use
	 * activity is low at least one minute
	 * 
	 * hub is disabled
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDisableHubIfMaxSessionsReached() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		doReturn(2).when(proxy).getUsedTestSlots();
		doReturn(11).when(proxy).getHubTotalTestSlots();

		LocalDateTime lowActivityBeginning = LocalDateTime.now().minusMinutes(1).minusSeconds(1);
		proxy.setHubStatus(GridStatus.ACTIVE);
		CustomRemoteProxy.setLowActivityBeginning(lowActivityBeginning);
		CustomRemoteProxy.setHubTestSessionCount(100);
		
		proxy.disableHubIfMaxSessionsReached();
		
		Assert.assertEquals(CustomRemoteProxy.getLowActivityBeginning(), lowActivityBeginning);
		Assert.assertEquals(proxy.getHubStatus(), GridStatus.INACTIVE);
	}
	
	/**
	 * Max hub session set and not reached
	 * less than 10% of test slots are in use
	 * activity is low at least one minute
	 * 
	 * hub is not disabled
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDoNotDisableHubIfMaxSessionsNotReached() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		doReturn(2).when(proxy).getUsedTestSlots();
		doReturn(11).when(proxy).getHubTotalTestSlots();
		
		LocalDateTime lowActivityBeginning = LocalDateTime.now().minusMinutes(1).minusSeconds(1);
		proxy.setHubStatus(GridStatus.ACTIVE);
		CustomRemoteProxy.setLowActivityBeginning(lowActivityBeginning);
		CustomRemoteProxy.setHubTestSessionCount(99);
		
		proxy.disableHubIfMaxSessionsReached();
		
		Assert.assertEquals(CustomRemoteProxy.getLowActivityBeginning(), lowActivityBeginning);
		Assert.assertEquals(proxy.getHubStatus(), GridStatus.ACTIVE); // never set before
	}
	
	/**
	 * Max hub session set and reached
	 * 10% of test slots are in use
	 * activity is low at least one minute
	 * 
	 * hub is not disabled
	 * low activity beginning time is reset
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDoNotDisableHubIfHighActivity() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		doReturn(2).when(proxy).getUsedTestSlots();
		doReturn(10).when(proxy).getHubTotalTestSlots();
		
		// low activity was recorded
		LocalDateTime lowActivityBeginning = LocalDateTime.now().minusMinutes(1).minusSeconds(1);
		proxy.setHubStatus(GridStatus.ACTIVE);
		CustomRemoteProxy.setLowActivityBeginning(lowActivityBeginning);
		CustomRemoteProxy.setHubTestSessionCount(100);
		
		proxy.disableHubIfMaxSessionsReached();
		
		Assert.assertEquals(CustomRemoteProxy.getLowActivityBeginning(), null);
		Assert.assertEquals(proxy.getHubStatus(), GridStatus.ACTIVE);
	}

	/**
	 * Max hub session set and reached
	 * less than 10% of test slots are in use
	 * activity was high just before 
	 * 
	 * hub is not disabled
	 * low activity beginning time is set
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDoNotDisableHubIfLowActivityNeverSet() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		doReturn(2).when(proxy).getUsedTestSlots();
		doReturn(11).when(proxy).getHubTotalTestSlots();
		
		// low activity was never recorded
		proxy.setHubStatus(GridStatus.ACTIVE);
		CustomRemoteProxy.setLowActivityBeginning(null);
		CustomRemoteProxy.setHubTestSessionCount(100);
		
		proxy.disableHubIfMaxSessionsReached();
		
		Assert.assertNotNull(CustomRemoteProxy.getLowActivityBeginning()); // date for low activity recorded
		Assert.assertEquals(proxy.getHubStatus(), GridStatus.ACTIVE); // never set before
	}

	/**
	 * Max hub session set and reached
	 * less than 10% of test slots are in use but this situation is quite new (< 1 min)
	 * 
	 * hub is not disabled
	 * low activity beginning time is reset
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDoNotDisableHubIfLowActivityIsNew() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		doReturn(2).when(proxy).getUsedTestSlots();
		doReturn(11).when(proxy).getHubTotalTestSlots();
		
		// low activity was recorded
		LocalDateTime lowActivityBeginning = LocalDateTime.now().minusSeconds(59);
		proxy.setHubStatus(GridStatus.ACTIVE);
		CustomRemoteProxy.setLowActivityBeginning(lowActivityBeginning);
		CustomRemoteProxy.setHubTestSessionCount(100);
		
		proxy.disableHubIfMaxSessionsReached();
		
		Assert.assertEquals(CustomRemoteProxy.getLowActivityBeginning(), lowActivityBeginning);
		Assert.assertEquals(proxy.getHubStatus(), GridStatus.ACTIVE);
	}

	/**
	 * Max hub session set and reached (2 times the threshold)
	 * more than 10% of test slots are in use
	 * activity is always high
	 * 
	 * hub is disabled because we won't wait indefinitely for activity to lower
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDisableHubIfTooManySession() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		doReturn(2).when(proxy).getUsedTestSlots();
		doReturn(3).when(proxy).getHubTotalTestSlots();
		
		// low activity was never recorded
		CustomRemoteProxy.setLowActivityBeginning(null);
		CustomRemoteProxy.setHubTestSessionCount(200);
		
		proxy.disableHubIfMaxSessionsReached();
		
		Assert.assertNull(CustomRemoteProxy.getLowActivityBeginning()); // date for low activity not recorded
		Assert.assertEquals(proxy.getHubStatus(), GridStatus.INACTIVE); // never set before
	}
	
	/**
	 * node is inactive (set by a call to disableNodeIfMaxSessionsReached)
	 * Max node session is set and reached
	 * node is not busy (not test session running)
	 * 
	 * node is stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testStopNodeIfInactiveAndMaxSessionReached() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(11);
		when(proxy.isBusy()).thenReturn(false);
		nodeStatus.put("status", GridStatus.INACTIVE.toString());
		
		proxy.stopNodeWithMaxSessionsReached();
		
		verify(nodeClient).stopNode();
	}
	
	/**
	 * node is inactive (set by a call to disableNodeIfMaxSessionsReached)
	 * Max node session is not set
	 * node is not busy (not test session running)
	 * 
	 * node is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDontStopNodeIfInactiveAndMaxSessionNotSet() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(null);
		proxy.setTestSessionsCount(11);
		when(proxy.isBusy()).thenReturn(false);
		nodeStatus.put("status", GridStatus.INACTIVE.toString());
		
		proxy.stopNodeWithMaxSessionsReached();
		
		verify(nodeClient, never()).stopNode();
	}

	/**
	 * node is inactive (set by a call to disableNodeIfMaxSessionsReached)
	 * Max node session is set and not reached
	 * node is not busy (not test session running)
	 * 
	 * node is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDontStopNodeIfInactiveAndMaxSessionNotReached() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(10);
		when(proxy.isBusy()).thenReturn(false);
		nodeStatus.put("status", GridStatus.INACTIVE.toString());
		
		proxy.stopNodeWithMaxSessionsReached();
		
		verify(nodeClient, never()).stopNode();
	}

	/**
	 * node is inactive (set by a call to disableNodeIfMaxSessionsReached)
	 * Max node session is set and reached
	 * node is busy (test session running)
	 * 
	 * node is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDontStopNodeIfBusyAndMaxSessionReached() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(11);
		when(proxy.isBusy()).thenReturn(true);
		nodeStatus.put("status", GridStatus.INACTIVE.toString());
		
		proxy.stopNodeWithMaxSessionsReached();
		
		verify(nodeClient, never()).stopNode();
	}
	/**
	 * node is active
	 * Max node session is set and reached
	 * node is busy (test session running)
	 * 
	 * node is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDontStopNodeIfActiveAndMaxSessionReached() throws UnirestException {
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(11);
		when(proxy.isBusy()).thenReturn(false);
		nodeStatus.put("status", GridStatus.ACTIVE.toString());
		
		proxy.stopNodeWithMaxSessionsReached();
		
		verify(nodeClient, never()).stopNode();
	}

	/**
	 * hub is inactive (set by a call to disableHubIfMaxSessionsReached)
	 * Max hub session is set and reached
	 * hub is not busy (no test session running)
	 * 
	 * hub is stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testStopHubIfInactiveAndMaxSessionReached() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		CustomRemoteProxy.setHubTestSessionCount(101);
		doReturn(0).when(proxy).getUsedTestSlots();
		proxy.setHubStatus(GridStatus.INACTIVE);
		
		proxy.stopHubWithMaxSessionsReached();
		
		verify(hub).stop();
	}
	
	/**
	 * hub is inactive (set by a call to disableHubIfMaxSessionsReached)
	 * Max hub session is set and reached
	 * hub is busy (test session running)
	 * 
	 * hub is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDontStopHubIfBusyAndMaxSessionReached() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		CustomRemoteProxy.setHubTestSessionCount(101);
		doReturn(1).when(proxy).getUsedTestSlots();
		proxy.setHubStatus(GridStatus.INACTIVE);
		
		proxy.stopHubWithMaxSessionsReached();
		
		verify(hub, never()).stop();
	}

	/**
	 * hub is inactive do to user action
	 * Max hub session is set and not reached
	 * hub is not busy (no test session running)
	 * 
	 * hub is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testDontStopHubIfInactiveAndMaxSessionNotReached() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		CustomRemoteProxy.setHubTestSessionCount(100);
		doReturn(0).when(proxy).getUsedTestSlots();
		proxy.setHubStatus(GridStatus.INACTIVE);
		
		proxy.stopHubWithMaxSessionsReached();
		
		verify(hub, never()).stop();
	}
	
	/**
	 * hub is inactive do to user action
	 * Max hub session is not set
	 * hub is not busy (no test session running)
	 * 
	 * hub is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testStopHubIfInactiveAndMaxSessionNotSet() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(null);
		CustomRemoteProxy.setHubTestSessionCount(101);
		doReturn(0).when(proxy).getUsedTestSlots();
		proxy.setHubStatus(GridStatus.INACTIVE);
		
		proxy.stopHubWithMaxSessionsReached();
		
		verify(hub, never()).stop();
	}

	/**
	 * hub is active (activity was too high for example)
	 * Max hub session is set and reached
	 * hub is not busy (no test session running)
	 * 
	 * hub is not stopped
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testStopHubIfActiveAndMaxSessionReached() throws UnirestException {
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		CustomRemoteProxy.setHubTestSessionCount(101);
		doReturn(0).when(proxy).getUsedTestSlots();
		proxy.setHubStatus(GridStatus.ACTIVE);
		
		proxy.stopHubWithMaxSessionsReached();
		
		verify(hub, never()).stop();
	}
	
	/**
	 * Check node stopping may be done each time isAlive is called 
	 * This method is called every 5 seconds even if no new session is created
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testIsAliveStopsNode() throws UnirestException {
		
		// place proxy in configuration where node should be stopped
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(11);
		when(proxy.isBusy()).thenReturn(false);
		doReturn(true).when(proxy).isProxyAlive();
		nodeStatus.put("status", GridStatus.INACTIVE.toString());
		
		proxy.isAlive();
		
		verify(nodeClient).stopNode();
	}
	
	/**
	 * When node is not alive, do not try to stop it
	 * @throws UnirestException
	 */
	@Test(groups={"grid"})
	public void testIsNotAliveDoesNotStopNode() throws UnirestException {
		
		// place proxy in configuration where node should be stopped
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(11);
		when(proxy.isBusy()).thenReturn(false);
		doReturn(false).when(proxy).isProxyAlive();
		nodeStatus.put("status", GridStatus.INACTIVE.toString());
		
		proxy.isAlive();
		
		verify(nodeClient, never()).stopNode();
	}
	
	/**
	 * Check hub stopping may be done each time isAlive is called 
	 * This method is called every 5 seconds even if no new session is created
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testIsAliveStopsHub() throws UnirestException {
		
		// place proxy in configuration where hub should be stopped
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		CustomRemoteProxy.setHubTestSessionCount(101);
		doReturn(0).when(proxy).getUsedTestSlots();
		proxy.setHubStatus(GridStatus.INACTIVE);
		
		proxy.isAlive();
		
		verify(hub).stop();
	}
	
	/**
	 * Check node may be disabled after a session
	 * @throws UnirestException
	 */
	@Test(groups={"grid"})
	public void testAfterSessionDisablesNode() throws UnirestException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		testSession.setExternalKey(new ExternalSessionKey("340c2e79e402ce6e6396df4d8140282a"));
		testSession.put(CustomRemoteProxy.PIDS_TO_KILL, Arrays.asList(2000L, 2010L));
		
		// put proxy in a state where node should be disabled
		when(launchConfig.getMaxNodeTestCount()).thenReturn(10);
		proxy.setTestSessionsCount(10);
		
		proxy.afterSession(testSession);
		
		verify(nodeStatusClient).setStatus(GridStatus.INACTIVE);
	}
	
	/**
	 * Check hub may be disabled after a session
	 * @throws UnirestException
	 */
	@Test(groups={"grid"})
	public void testAfterSessionDisablesHub() throws UnirestException {
		Map<String, Object> requestedCapabilities = new HashMap<>();
		requestedCapabilities.put("browserName", "chrome");
		requestedCapabilities.put("browserVersion", "67.0");
		TestSession testSession = new TestSession(testSlot1, requestedCapabilities, Clock.systemUTC());
		testSession.setExternalKey(new ExternalSessionKey("340c2e79e402ce6e6396df4d8140282a"));
		testSession.put(CustomRemoteProxy.PIDS_TO_KILL, Arrays.asList(2000L, 2010L));
		
		// put proxy in a state where hub should be disabled
		when(launchConfig.getMaxHubTestCount()).thenReturn(100);
		doReturn(1).when(proxy).getUsedTestSlots();
		doReturn(11).when(proxy).getHubTotalTestSlots();

		LocalDateTime lowActivityBeginning = LocalDateTime.now().minusMinutes(1).minusSeconds(1);
		proxy.setHubStatus(GridStatus.ACTIVE);
		CustomRemoteProxy.setLowActivityBeginning(lowActivityBeginning);
		CustomRemoteProxy.setHubTestSessionCount(100);
		
		proxy.afterSession(testSession);
		
		Assert.assertEquals(proxy.getHubStatus(), GridStatus.INACTIVE);
	}
	

	/**
	 * check we clean node each time isAlive is called if proxy is not busy
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testIsAliveCleansNodeIfNotBusy() throws UnirestException {

		when(proxy.isBusy()).thenReturn(false);
		doReturn(true).when(proxy).isProxyAlive();
		
		proxy.isAlive();
		
		verify(nodeClient).cleanNode();
	}
	
	/**
	 * check we do not clean node if proxy is busy
	 * @throws UnirestException 
	 */
	@Test(groups={"grid"})
	public void testIsAliveDoesNotCleanNodeIfBusy() throws UnirestException {
		
		when(proxy.isBusy()).thenReturn(true);
		doReturn(true).when(proxy).isProxyAlive();
		
		proxy.isAlive();
		
		verify(nodeClient, never()).cleanNode();
	}
	
	@Test(groups= {"grid"})
	public void testCreateMobileTestSlot() {

		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		caps.put("deviceName", "device1");
		caps.put(MobileCapabilityType.PLATFORM_NAME, "ios");
		
		TestSlot slot = proxy.createTestSlot(SeleniumProtocol.WebDriver, caps);
		Assert.assertTrue(slot.getProxy() instanceof CustomRemoteProxyWrapper);
		Assert.assertTrue(((CustomRemoteProxyWrapper)slot.getProxy()).isMobileSlot());
	}
	
	@Test(groups= {"grid"})
	public void testCreateDesktopTestSlot() throws MalformedURLException {
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		caps.put("deviceName", "device1");
		
		TestSlot slot = proxy.createTestSlot(SeleniumProtocol.WebDriver, caps);
		Assert.assertTrue(slot.getProxy() instanceof CustomRemoteProxyWrapper);
		CustomRemoteProxyWrapper wrapper = (CustomRemoteProxyWrapper)slot.getProxy();
		Assert.assertFalse(wrapper.isMobileSlot());
	}
	
	/**
	 * Test we get a new session from testSlot. This is the standard case
	 */
	@Test(groups={"grid"})
	public void testGetNewSession() {
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		
		when(proxy.getTestSlots()).thenReturn(Arrays.asList(testSlot1, testSlot2));
		when(testSlot1.getNewSession(eq(caps))).thenReturn(testSession);
		when(testSlot1.matches(eq(caps))).thenReturn(true);
		
		TestSession session = proxy.getNewSession(caps);
		Assert.assertNotNull(session);
	}
	
	/**
	 * Test we cannot create new session if maxSession is reached (max number of parallel sessions per node)
	 */
	@Test(groups={"grid"})
	public void testGetNewSessionMaxSessionReached() {
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		
		proxy.getConfig().maxSession = 1;
		when(proxy.getTestSlots()).thenReturn(Arrays.asList(testSlot1, testSlot2));
		when(testSlot1.getNewSession(eq(caps))).thenReturn(testSession);
		when(testSlot1.matches(eq(caps))).thenReturn(true);
		when(testSlot2.getNewSession(eq(caps))).thenReturn(testSession);
		when(testSlot2.matches(eq(caps))).thenReturn(true);
		
		TestSession session1 = proxy.getNewSession(caps);
		when(testSlot1.getSession()).thenReturn(session1); // simulate that slot1 is used
		TestSession session2 = proxy.getNewSession(caps);
		
		// first session is created, but not the second one because maxSession = 1
		Assert.assertNotNull(session1);
		Assert.assertNull(session2);
	}
	
	/**
	 * isue #45: test we can create a new session on the same node as an other one, even if maxSession is reached
	 * 	This will allow to start 2 browsers of the same scenario, on the same node
	 */
	@Test(groups={"grid"})
	public void testGetNewSessionAttachingOnNode() {
		Map<String, Object> caps = new HashMap<>();
		Map<String, Object> caps2 = new HashMap<>();
		caps2.put(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://0.0.0.0:-1");
		
		proxy.getConfig().maxSession = 1;
		when(proxy.getTestSlots()).thenReturn(Arrays.asList(testSlot1, testSlot2));
		when(testSlot1.getNewSession(eq(caps))).thenReturn(testSession);
		when(testSlot1.matches(eq(caps))).thenReturn(true);
		when(testSlot2.getNewSession(eq(caps2))).thenReturn(testSession);
		when(testSlot2.matches(eq(caps2))).thenReturn(true);
		
		TestSession session1 = proxy.getNewSession(caps);
		when(testSlot1.getSession()).thenReturn(session1); // simulate that slot1 is used
		TestSession session2 = proxy.getNewSession(caps2);
		
		// first session is created, and also second one because we requested to attach the new session to node.
		Assert.assertNotNull(session1);
		Assert.assertNotNull(session2);
		Assert.assertEquals(proxy.getConfig().maxSession, (Integer)1);
	}
	
	/**
	 * Check that even if capabilities are matching, it's not possible to attach to an other node than the requested one when specifying 'ATTACH_SESSION_ON_NODE'
	 */
	@Test(groups={"grid"})
	public void testGetNewSessionNotAttachingOnOtherNode() {

		Map<String, Object> caps = new HashMap<>();
		Map<String, Object> caps2 = new HashMap<>();
		caps2.put(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://1.1.1.1:-1");
		
		proxy.getConfig().maxSession = 1;
		when(proxy.getTestSlots()).thenReturn(Arrays.asList(testSlot1, testSlot2));
		when(testSlot1.getNewSession(eq(caps))).thenReturn(testSession);
		when(testSlot1.matches(eq(caps))).thenReturn(true);
		when(testSlot2.getNewSession(eq(caps2))).thenReturn(testSession);
		when(testSlot2.matches(eq(caps2))).thenReturn(true);
		
		TestSession session1 = proxy.getNewSession(caps);
		when(testSlot1.getSession()).thenReturn(session1); // simulate that slot1 is used
		TestSession session2 = proxy.getNewSession(caps2);
		
		// first session is created, second one is not because node URL do not match
		Assert.assertNotNull(session1);
		Assert.assertNull(session2);
		Assert.assertEquals(proxy.getConfig().maxSession, (Integer)1);
	}
	
	/**
	 * When there are not enough slots to create the new driver, do not create
	 */
	@Test(groups={"grid"})
	public void testGetNewSessionNotAttachingIfNotEnoughSlots() {
		
		Map<String, Object> caps = new HashMap<>();
		Map<String, Object> caps2 = new HashMap<>();
		caps2.put(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://0.0.0.0:-1");
		
		proxy.getConfig().maxSession = 1;
		when(proxy.getTestSlots()).thenReturn(Arrays.asList(testSlot1));
		when(testSlot1.getNewSession(eq(caps))).thenReturn(testSession);
		when(testSlot1.matches(eq(caps))).thenReturn(true);
		when(testSlot1.matches(eq(caps2))).thenReturn(true);
		
		TestSession session1 = proxy.getNewSession(caps);
		when(testSlot1.getSession()).thenReturn(session1); // simulate that slot1 is used
		TestSession session2 = proxy.getNewSession(caps2);
		
		// first session is created, second one is not because node URL do not match
		Assert.assertNotNull(session1);
		Assert.assertNull(session2);
		Assert.assertEquals(proxy.getConfig().maxSession, (Integer)1);
	}
}
