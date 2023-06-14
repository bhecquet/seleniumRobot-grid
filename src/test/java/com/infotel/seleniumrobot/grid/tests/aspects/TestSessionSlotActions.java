package com.infotel.seleniumrobot.grid.tests.aspects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
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
import org.aspectj.lang.ProceedingJoinPoint;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.node.ActiveSession;
import org.openqa.selenium.grid.node.local.SessionSlot;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.SessionId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.aspects.SessionSlotActions;
import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.client.NodeClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.tasks.DiscoverBrowserAndDriverPidsTask;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.video.StopVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.util.helper.WaitHelper;

import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

@PrepareForTest({Utils.class, LaunchConfig.class, DiscoverBrowserAndDriverPidsTask.class, SessionSlotActions.class, StopVideoCaptureTask.class, KillTask.class})
@PowerMockIgnore("javax.management.*")
public class TestSessionSlotActions extends BaseMockitoTest {


	@Mock
	DiscoverBrowserAndDriverPidsTask discoverBrowserAndDriverPidsTask;
	
	@Mock
	StopVideoCaptureTask stopVideoCaptureTask;
	
	@Mock
	KillTask killTask;
	
	@Mock
	LaunchConfig launchConfig;
	
	@Mock
	GridNodeConfiguration gridNodeConfiguration;
	
	@Mock
	BaseServerOptions baseServerOptions;
	
	@Mock
	NodeTaskServletClient nodeClient;
	
	@Mock
	NodeClient nodeStatusClient;

	
	@Mock
	HttpServletRequest servletRequest;
	
	@Mock
	HttpServletResponse servletResponse;
	
	@Mock
	CreateSessionRequest createSessionRequest;
	
	@Mock
	CreateSessionRequest createSessionRequest2;
	
	@Mock
	SessionSlot sessionSlot;
	
	@Mock
	ActiveSession activeSession;
	
	@Mock
	ProceedingJoinPoint joinPoint;
	
	MutableCapabilities firefoxCaps;
	MutableCapabilities chromeCaps;
	MutableCapabilities edgeCaps;
	MutableCapabilities ieCaps;
	
	SessionSlotActions slotActions;
	
	@BeforeMethod(groups={"grid"})
	public void setup() throws Exception {
		PowerMockito.mockStatic(Utils.class);
		PowerMockito.mockStatic(DiscoverBrowserAndDriverPidsTask.class);
		PowerMockito.mockStatic(StopVideoCaptureTask.class);
		PowerMockito.mockStatic(KillTask.class);
		
		when(joinPoint.getArgs()).thenReturn(new Object[] {createSessionRequest}); // to mock 'onNewSession'
		when(joinPoint.getThis()).thenReturn(sessionSlot);
		
		firefoxCaps = new MutableCapabilities();
		firefoxCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.FIREFOX.browserName());
		firefoxCaps.setCapability(CapabilityType.BROWSER_VERSION, "100.0");
		chromeCaps = new MutableCapabilities();
		chromeCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
		chromeCaps.setCapability(CapabilityType.BROWSER_VERSION, "101.0");
		edgeCaps = new MutableCapabilities();
		edgeCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.EDGE.browserName());
		edgeCaps.setCapability(CapabilityType.BROWSER_VERSION, "102.0");
		ieCaps = new MutableCapabilities();
		ieCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		ieCaps.setCapability(CapabilityType.BROWSER_VERSION, "11.0");
		
		when(sessionSlot.getSession()).thenReturn(activeSession);
		when(sessionSlot.getStereotype()).thenReturn(firefoxCaps);
		when(activeSession.getId()).thenReturn(new SessionId("1234"));
		
		slotActions = spy(new SessionSlotActions(1, nodeStatusClient));

		when(nodeStatusClient.isBusyOnOtherSlot(null)).thenReturn(true); // by default, do not clean
		
		// tasks
		PowerMockito.whenNew(DiscoverBrowserAndDriverPidsTask.class).withAnyArguments().thenReturn(discoverBrowserAndDriverPidsTask);
		when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
		when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
		when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
		when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		
		PowerMockito.whenNew(StopVideoCaptureTask.class).withAnyArguments().thenReturn(stopVideoCaptureTask);
		
		PowerMockito.whenNew(KillTask.class).withAnyArguments().thenReturn(killTask);
		when(killTask.withPid(anyLong())).thenReturn(killTask);
	}
	
	/**
	 * Check "onNewSession" does actions before and after new session call
	 * @throws Throwable 
	 */
	@Test(groups={"grid"})
	public void testOnNewSession() throws Throwable {
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		doReturn(createSessionRequest2).when(slotActions).beforeStartSession(createSessionRequest, sessionSlot);
		
		slotActions.onNewSession(joinPoint);
		verify(slotActions).beforeStartSession(createSessionRequest, sessionSlot);
		verify(slotActions).afterStartSession(new SessionId("1234"), sessionSlot);
		verify(joinPoint).proceed(new Object[] {createSessionRequest2}); // check we use the new session request
	}
	
	/**
	 * Check afterStartSession is still called if error occurs when creating the session
	 * @throws Throwable
	 */
	@Test(groups={"grid"})
	public void testOnNewSessionErrorInProceed() throws Throwable {
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		doReturn(createSessionRequest2).when(slotActions).beforeStartSession(createSessionRequest, sessionSlot);
		when(joinPoint.proceed(new Object[] {createSessionRequest2})).thenThrow(new WebDriverException("error"));
		
		try {
			slotActions.onNewSession(joinPoint);
		} catch (WebDriverException e) {}
		verify(slotActions).beforeStartSession(createSessionRequest, sessionSlot);
		verify(slotActions).afterStartSession(new SessionId("1234"), sessionSlot);
		
	}
	
	@Test(groups={"grid"})
	public void testOnStopSession() throws Throwable {

		slotActions.onStopSession(joinPoint);
		verify(slotActions).beforeStopSession(new SessionId("1234"), sessionSlot);
		verify(slotActions).afterStopSession(new SessionId("1234"));
	}
	
	@Test(groups={"grid"})
	public void testOnStopSessionNoSession() throws Throwable {
		
		when(sessionSlot.getSession()).thenThrow(new NoSuchSessionException(""));
		
		slotActions.onStopSession(joinPoint);
		verify(slotActions).beforeStopSession(null, sessionSlot);
		verify(slotActions).afterStopSession(null);
	}
	
//	/**
//	 * check that file path is updated with a remote URL
//	 */
//	@Test(groups={"grid"})
//	public void testOnNewSessionChangeUploadedFilePath() {
//		
//		
//		
//		Map<String, Object> caps = new HashMap<>();
//		caps.put("key", "file:aFolder/aFile");
//		
//		
//		when(newSessionRequest.getDesiredCapabilities()).thenReturn(caps);
//		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
//		Assert.assertEquals(caps.get("key"), "http://localhost:4444/grid/admin/FileServlet/aFolder/aFile");
//	}
//	
//	/**
//	 * Test that when 'platformName' is defined, we call mobileServlet to update capabilities with node caps
//	 * It allows to switch from a human readable name to an ID on android
//	 * @throws ClientProtocolException
//	 * @throws IOException
//	 * @throws URISyntaxException
//	 */
//	@Test(groups={"grid"})
//	public void testOnNewSessionUpdateMobileDeviceName() {
//		
//		
//		
//		Map<String, Object> caps = new HashMap<>();
//		caps.put("key", "value");
//		caps.put("deviceName", "device1");
//		caps.put(MobileCapabilityType.PLATFORM_NAME, "ios");
//		
//		DesiredCapabilities newCaps = new DesiredCapabilities(caps);
//		newCaps.setCapability("deviceName", "id-1234");
//		
//		when(mobileServletClient.updateCapabilities(new DesiredCapabilities(caps))).thenReturn(newCaps);
//		
//		when(newSessionRequest.getDesiredCapabilities()).thenReturn(caps);
//		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
//		Assert.assertEquals(caps.get("key"), "value");
//		Assert.assertEquals(caps.get("deviceName"), "id-1234");
//	}	
//	
	/**
	 * Check we clean if no other slot is busy
	 */
	@Test(groups={"grid"})
	public void testBeforeStartSessionClean() {
		when(nodeStatusClient.isBusyOnOtherSlot(null)).thenReturn(false);
		
		Map<String, Object> caps = new HashMap<>();
		caps.put(CapabilityType.PLATFORM_NAME, Platform.VISTA);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		
		slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		verify(slotActions).cleanNode();
	}	
	/**
	 * Check list of PIDs is stored before starting the session
	 */
	@Test(groups={"grid"})
	public void testBeforeStartSessionGetPids() {

		Map<String, Object> caps = new HashMap<>();
		caps.put(CapabilityType.PLATFORM_NAME, Platform.VISTA);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

		verify(slotActions).setPreexistingBrowserAndDriverPids(sessionSlot, Arrays.asList(10L, 20L));
	}	
	
	/**
	 * issue #54: Test that when 'platform' is defined with precise OS, for desktop tests, we change platform and platformName capabilities 
	 * Here, Windows 7 (Vista) => Windows
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testOnNewSessionUpdatePlatformWindow7Caps() {

		Map<String, Object> caps = new HashMap<>();
		caps.put(CapabilityType.PLATFORM_NAME, Platform.VISTA);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		Assert.assertEquals(newSessionRequest.getDesiredCapabilities().getCapability(CapabilityType.PLATFORM_NAME), Platform.WINDOWS);
	}	
	
	/**
	 * issue #54: Test that when 'platform' is defined with general OS, for desktop tests, we do not change platform and platformName capabilities 
	 * Here, Windows  => Windows
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testOnNewSessionUpdatePlatformWindowCaps() {
		Map<String, Object> caps = new HashMap<>();
		caps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		Assert.assertEquals(newSessionRequest.getDesiredCapabilities().getCapability(CapabilityType.PLATFORM_NAME), Platform.WINDOWS);
	}	
	
	/**
	 * Test that chrome driver path is added to session capabilities
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testChromeDriverAdded() {
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));
		
		Map<String, Object> chromeOptions = new HashMap<>();
		chromeOptions.put("binary", "/home/chrome");
		chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		when(sessionSlot.getStereotype()).thenReturn(chromeCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		// issue #60: check binary is also there
		Assert.assertEquals(((Map<String, Object>)newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("binary"), "/home/chrome");
	}
	
	@Test(groups={"grid"})
	public void testChromeDefaultProfileAdded() {

		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
		requestedCaps.put(SeleniumRobotCapabilityType.CHROME_PROFILE, "default");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

		Map<String, Object> chromeOptions = new HashMap<>();
		chromeOptions.put("binary", "/home/chrome");
		chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		chromeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/chrome/profile");
		when(sessionSlot.getStereotype()).thenReturn(chromeCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		Assert.assertTrue(((Map<String, List<String>>)newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/chrome/profile"));
	}
	
	@Test(groups={"grid"})
	public void testChromeUserProfileAdded() {

		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
		requestedCaps.put(SeleniumRobotCapabilityType.CHROME_PROFILE, "/home/user/myprofile");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

		Map<String, Object> chromeOptions = new HashMap<>();
		chromeOptions.put("binary", "/home/chrome");
		chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		chromeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/chrome/profile");
		when(sessionSlot.getStereotype()).thenReturn(chromeCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		Assert.assertTrue(((Map<String, List<String>>)newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/user/myprofile"));
	}
	
	@Test(groups={"grid"})
	public void testChromeNoUserProfileAdded() {
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

		Map<String, Object> chromeOptions = new HashMap<>();
		chromeOptions.put("binary", "/home/chrome");
		chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		chromeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/chrome/profile");
		when(sessionSlot.getStereotype()).thenReturn(chromeCaps);

		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		Assert.assertEquals(((Map<String, List<String>>)newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("args").size(), 0);
	}
	
	@Test(groups={"grid"})
	public void testEdgeDriverAdded() {
	
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));
		
		Map<String, Object> edgeOptions = new HashMap<>();
		edgeOptions.put("binary", "/home/edge");
		edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
		when(sessionSlot.getStereotype()).thenReturn(edgeCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		// issue #60: check binary is also there
		Assert.assertEquals(((Map<String, Object>)newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("binary"), "/home/edge");
	}
	
	@Test(groups={"grid"})
	public void testEdgeDefaultProfileAdded() {
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.toString());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(EdgeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(EdgeOptions.CAPABILITY)).put("args", new ArrayList<>());
		requestedCaps.put(SeleniumRobotCapabilityType.EDGE_PROFILE, "default");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));
		
		Map<String, Object> edgeOptions = new HashMap<>();
		edgeOptions.put("binary", "/home/edge");
		edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
		edgeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/edge/profile");
		when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		Assert.assertTrue(((Map<String, List<String>>)newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/edge/profile"));
	}
	
	@Test(groups={"grid"})
	public void testEdgeUserProfileAdded() {

		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.toString());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(EdgeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(EdgeOptions.CAPABILITY)).put("args", new ArrayList<>());
		requestedCaps.put(SeleniumRobotCapabilityType.EDGE_PROFILE, "/home/user/myprofile");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

		Map<String, Object> edgeOptions = new HashMap<>();
		edgeOptions.put("binary", "/home/edge");
		edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
		edgeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/edge/profile");
		when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		Assert.assertTrue(((Map<String, List<String>>)newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/user/myprofile"));
	}
	
	@Test(groups={"grid"})
	public void testEdgeNoUserProfileAdded() {
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.toString());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(EdgeOptions.CAPABILITY, new HashMap<String, String>());
		((Map<String, List<String>>)requestedCaps.get(EdgeOptions.CAPABILITY)).put("args", new ArrayList<>());
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

		Map<String, Object> edgeOptions = new HashMap<>();
		edgeOptions.put("binary", "/home/edge");
		edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
		edgeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/edge/profile");
		when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		Assert.assertEquals(((Map<String, List<String>>)newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("args").size(), 0);
	}

	/**
	 * Check profile has been updated ('firefoxProfile' cap is  set to default). Some initial preferences are kept 'general.useragent.override' and 'network.automatic-ntlm-auth.trusted-uris'
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testFirefoxDefaultProfileAdded() throws IOException {

		FirefoxOptions requestedCaps = new FirefoxOptions();
		requestedCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.FIREFOX.toString());
		requestedCaps.setCapability(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.setProfile(FirefoxProfile.fromJson("{}"));
		requestedCaps.setCapability(SeleniumRobotCapabilityType.FIREFOX_PROFILE, "default");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(requestedCaps);
		
		firefoxCaps.setCapability(FirefoxDriver.SystemProperty.BROWSER_BINARY, "/home/firefox");
		firefoxCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/firefox/profile");
		when(sessionSlot.getStereotype()).thenReturn(firefoxCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		FirefoxProfile newProfile = new FirefoxOptions(newSessionRequest.getDesiredCapabilities()).getProfile();
		
		// check default preferences
		Assert.assertEquals(newProfile.getStringPreference("general.useragent.override", "no"), "no");
		Assert.assertEquals(newProfile.getStringPreference("network.automatic-ntlm-auth.trusted-uris", "no"), "no");
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Window.QueryInterface", ""), SessionSlotActions.ALL_ACCESS);
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Window.frameElement.get", ""),  SessionSlotActions.ALL_ACCESS);
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.HTMLDocument.compatMode.get", ""),  SessionSlotActions.ALL_ACCESS);
		Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Document.compatMode.get", ""),  SessionSlotActions.ALL_ACCESS);
		Assert.assertEquals(newProfile.getIntegerPreference("dom.max_chrome_script_run_time", 10), 0);
        Assert.assertEquals(newProfile.getIntegerPreference("dom.max_script_run_time", 10), 0);
	}
	
	/**
	 * Check 'general.useragent.override', 'network.automatic-ntlm-auth.trusted-uris' are kept from provided profile
	 */
	@Test(groups={"grid"})
	public void testFirefoxDefaultProfileAdded2() {

		FirefoxOptions requestedCaps = new FirefoxOptions();
		requestedCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.FIREFOX.toString());
		requestedCaps.setCapability(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("general.useragent.override", "ua");
		profile.setPreference("network.automatic-ntlm-auth.trusted-uris", "uri");
		profile.setPreference("mypref", "mp");
		requestedCaps.setProfile(profile);
		requestedCaps.setCapability(SeleniumRobotCapabilityType.FIREFOX_PROFILE, "default");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(requestedCaps);

		firefoxCaps.setCapability(FirefoxDriver.SystemProperty.BROWSER_BINARY, "/home/firefox");
		firefoxCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/firefox/profile");
		when(sessionSlot.getStereotype()).thenReturn(firefoxCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

		FirefoxProfile newProfile = new FirefoxOptions(newSessionRequest.getDesiredCapabilities()).getProfile();
		
		// check updated preferences
		Assert.assertEquals(newProfile.getStringPreference("mypref", "no"), "no");
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
	public void testFirefoxNoDefaultProfile() {
		FirefoxOptions requestedCaps = new FirefoxOptions();
		requestedCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.FIREFOX.toString());
		requestedCaps.setCapability(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("mypref", "mp");
		requestedCaps.setProfile(profile);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(requestedCaps);
		
		firefoxCaps.setCapability(FirefoxDriver.SystemProperty.BROWSER_BINARY, "/home/firefox");
		firefoxCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/firefox/profile");
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		when(sessionSlot.getStereotype()).thenReturn(firefoxCaps);

		FirefoxProfile newProfile = new FirefoxOptions(newSessionRequest.getDesiredCapabilities()).getProfile();
		
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
	public void testIeDriverAdded() {
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

		// check Edge keys are not there
		Assert.assertNull(newSessionRequest.getDesiredCapabilities().getCapability("ie.edgechromium"));
		Assert.assertNull(newSessionRequest.getDesiredCapabilities().getCapability("ie.edgepath"));
		Assert.assertNull(newSessionRequest.getDesiredCapabilities().getCapability(SessionSlotActions.SE_IE_OPTIONS));
	}
	
	/**
	 * Check capabilities for testing Edge in IE mode are there
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testEdgeIeModeCapabilitiesAdded() {
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, true);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));
		
		// Edge available
		ieCaps.setCapability(SessionSlotActions.EDGE_PATH, "C:\\msedge.exe");
		when(sessionSlot.getStereotype()).thenReturn(ieCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		
		Assert.assertEquals(newSessionRequest.getDesiredCapabilities().getCapability("ie.edgechromium"), true);
		Assert.assertEquals(((Map<String, Object>)newSessionRequest.getDesiredCapabilities().getCapability(SessionSlotActions.SE_IE_OPTIONS)).get("ie.edgechromium"), true);
		Assert.assertEquals(newSessionRequest.getDesiredCapabilities().getCapability("ie.edgepath"), "C:\\msedge.exe");
		Assert.assertEquals(((Map<String, Object>)newSessionRequest.getDesiredCapabilities().getCapability(SessionSlotActions.SE_IE_OPTIONS)).get("ie.edgepath"), "C:\\msedge.exe");
	}
	
	/**
	 * When Edge in IE mode is requested, but not available, do not add keys
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test(groups={"grid"})
	public void testEdgeIeModeCapabilitiesNotAdded() {
		
		Map<String, Object> requestedCaps = new HashMap<>();
		requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
		requestedCaps.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, true);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));
		
		// Edge available
		when(sessionSlot.getStereotype()).thenReturn(ieCaps);
		
		CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

		// check Edge keys are not there
		Assert.assertNull(newSessionRequest.getDesiredCapabilities().getCapability("ie.edgechromium"));
		Assert.assertNull(newSessionRequest.getDesiredCapabilities().getCapability("ie.edgepath"));
		Assert.assertNull(newSessionRequest.getDesiredCapabilities().getCapability(SessionSlotActions.SE_IE_OPTIONS));
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
		when(sessionSlot.getStereotype()).thenReturn(new DesiredCapabilities(nodeCaps));
		
	}
	
	@Test(groups={"grid"})
	public void testAfterStartSessionExistingPid() {
		when(slotActions.getPreexistingBrowserAndDriverPids(new SessionId("2345"))).thenReturn(Arrays.asList(30L, 40L));

		when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		
		slotActions.afterStartSession(new SessionId("2345"), sessionSlot);
		
		// verify we store only the new PIDS
		verify(slotActions).setCurrentBrowserAndDriverPids(new SessionId("2345"), Arrays.asList(10L, 20L));
		
		// remove existing pids for this session
		verify(slotActions).removePreexistingPidsForSession(new SessionId("2345"));
	}
	
	@Test(groups={"grid"})
	public void testAfterStartSessionNoExistingPid() {
		when(slotActions.getPreexistingBrowserAndDriverPids(new SessionId("2345"))).thenReturn(null);
		
		when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		
		slotActions.afterStartSession(new SessionId("2345"), sessionSlot);
		
		// verify we store only the new PIDS
		verify(slotActions).setCurrentBrowserAndDriverPids(new SessionId("2345"), new ArrayList<>());
	}
	
	@Test(groups={"grid"})
	public void testBeforeStopSession() throws Exception {
		
		when(slotActions.getCurrentBrowserAndDriverPids(new SessionId("2345"))).thenReturn(Arrays.asList(10L));
		
		slotActions.beforeStopSession(new SessionId("2345"), sessionSlot);
		
		verify(stopVideoCaptureTask).execute();
		verify(discoverBrowserAndDriverPidsTask).withParentsPids(Arrays.asList(10L)); // we use the current pids (browser and driver) to get the list of all PIDs created by the browser
		verify(slotActions).setPidsToKill(new SessionId("2345"), Arrays.asList(10L, 20L)); // kill process that has been discovered
		verify(slotActions).removeCurrentBrowserPids(new SessionId("2345")); // we have used the current pids, now delete them
	}
	
	/**
	 * Nothing should happen
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testBeforeStopSessionErrorStoppingVideoCapture() throws Exception {
		
		when(slotActions.getCurrentBrowserAndDriverPids(new SessionId("2345"))).thenReturn(Arrays.asList(10L));
		when(stopVideoCaptureTask.execute()).thenThrow(new IOException("file error"));
		
		slotActions.beforeStopSession(new SessionId("2345"), sessionSlot);
		
		verify(stopVideoCaptureTask).execute();
		verify(discoverBrowserAndDriverPidsTask).withParentsPids(Arrays.asList(10L)); // we use the current pids (browser and driver) to get the list of all PIDs created by the browser
		verify(slotActions).setPidsToKill(new SessionId("2345"), Arrays.asList(10L, 20L)); // kill process that has been discovered
	}
	

	@Test(groups={"grid"})
	public void testAfterStopSession() throws Exception {
		
		when(slotActions.getPidsToKill(new SessionId("2345"))).thenReturn(Arrays.asList(10L, 20L));
		when(nodeStatusClient.isBusyOnOtherSlot("2345")).thenReturn(true); // do not clean
		
		slotActions.afterStopSession(new SessionId("2345"));
		
		// check pids are killed
		verify(killTask).withPid(10L); 
		verify(killTask).withPid(20L); 
		verify(killTask, times(2)).execute(); 
		verify(slotActions, never()).cleanNode();
	}
	
	/**
	 * Iff error occurs during kill, do not stop
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testAfterStopSessionErrorKilling() throws Exception {
		
		when(slotActions.getPidsToKill(new SessionId("2345"))).thenReturn(Arrays.asList(10L, 20L));
		when(nodeStatusClient.isBusyOnOtherSlot("2345")).thenReturn(true); // do not clean
		when(killTask.execute()).thenThrow(new IOException("error killing"));
		
		slotActions.afterStopSession(new SessionId("2345"));
		
		// check pids are killed
		verify(killTask).withPid(10L); 
		verify(killTask).withPid(20L); 
		verify(killTask, times(2)).execute(); 
	}
	
	@Test(groups={"grid"})
	public void testAfterStopSessionClean() throws Exception {
		
		when(slotActions.getPidsToKill(new SessionId("2345"))).thenReturn(Arrays.asList(10L, 20L));
		when(nodeStatusClient.isBusyOnOtherSlot("2345")).thenReturn(false); // do not clean
		
		slotActions.afterStopSession(new SessionId("2345"));
		 
		verify(slotActions).cleanNode();
	}
	

	@Test(groups={"grid"})
	public void testOnNewSession2() throws Throwable {
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		
		slotActions.onNewSession(joinPoint);
		verify(slotActions).beforeStartSession(createSessionRequest, sessionSlot);
		verify(slotActions).afterStartSession(new SessionId("1234"), sessionSlot);
	}
	
	/**
	 * Test that if several thread create a session on the same node, the second thread waits for first thread action terminating (session creation) before going on
	 * @throws Throwable 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 * @throws InterruptedException 
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingSession() throws Throwable {
		
		Answer<Object> proceedAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				WaitHelper.waitForSeconds(2);
				return "";
			}
		};
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		
		when(joinPoint.proceed(any(new Object[] {}.getClass()))).then(proceedAnswer);
		
		Clock clock = Clock.systemUTC();
		Instant start = clock.instant();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());

		SessionSlotActions slotActions2 = spy(new SessionSlotActions(30, nodeStatusClient));
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			try {
				slotActions2.onNewSession(joinPoint);
			} catch (Throwable e) {
			
			}
			ends.put(1, clock.instant().toEpochMilli() - start.toEpochMilli());
	      });
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			try {
				slotActions2.onNewSession(joinPoint);
			} catch (Throwable e) {
			}
			ends.put(2, clock.instant().toEpochMilli() - start.toEpochMilli());
		});
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		
		// check that thread 2 blocked for at least 2 seconds (meaning it started after 'afterCommand' call of thread 1)
		Assert.assertTrue(ends.get(2) > 2000);
		
	}
	@Test(groups={"grid"})
	public void concurrencyForCreatingSessionAfterStartSessionNotCalled() throws Throwable {
		
		Answer<Object> proceedAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				WaitHelper.waitForSeconds(2);
				return "";
			}
		};
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		
		when(joinPoint.proceed(any(new Object[] {}.getClass()))).then(proceedAnswer);
		
		Clock clock = Clock.systemUTC();
		Instant start = clock.instant();
		Map<Integer, Long> ends = Collections.synchronizedMap(new HashMap<>());
		
		SessionSlotActions slotActions2 = spy(new SessionSlotActions(5, nodeStatusClient));
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			slotActions2.beforeStartSession(createSessionRequest, sessionSlot);
			ends.put(1, clock.instant().toEpochMilli() - start.toEpochMilli());
		});
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			try {
				slotActions2.onNewSession(joinPoint);
			} catch (Throwable e) {
			}
			ends.put(2, clock.instant().toEpochMilli() - start.toEpochMilli());
		});
		executorService.shutdown();
		executorService.awaitTermination(150, TimeUnit.SECONDS);
		
		// check that thread 2 blocked for at least 5 seconds (meaning it started after 'afterCommand' call of thread 1)
		Assert.assertTrue(ends.get(2) > 7000); // 5 secs lock timeout + 2 seconds execution + 0.5 sec wait
		Assert.assertTrue(ends.get(2) < 8000);
		
	}
}
