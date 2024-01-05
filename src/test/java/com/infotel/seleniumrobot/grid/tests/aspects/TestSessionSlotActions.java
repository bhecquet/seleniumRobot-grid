package com.infotel.seleniumrobot.grid.tests.aspects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.Platform;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.node.ActiveSession;
import org.openqa.selenium.grid.node.local.SessionSlot;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.internal.Either;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.SessionId;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
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
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.osutility.OSUtility;

import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

public class TestSessionSlotActions extends BaseMockitoTest {

	
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
	
	@Mock
	ProceedingJoinPoint joinPointBeta;
	
	MutableCapabilities firefoxCaps;
	MutableCapabilities chromeCaps;
	MutableCapabilities edgeCaps;
	MutableCapabilities ieCaps;
	
	SessionSlotActions slotActions;

	private MockedStatic mockedUtils;
	private MockedStatic mockedOSUtility;

	@BeforeMethod(groups={"grid"})
	public void setup() throws Exception {
		mockedUtils = mockStatic(Utils.class);

		mockedOSUtility = mockStatic(OSUtility.class);

		mockedOSUtility.when(() -> OSUtility.getCurrentPlatorm()).thenReturn(Platform.LINUX);
		when(joinPoint.getArgs()).thenReturn(new Object[] {createSessionRequest}); // to mock 'onNewSession'
		when(joinPoint.getThis()).thenReturn(sessionSlot);
		when(joinPointBeta.getArgs()).thenReturn(new Object[] {createSessionRequest2}); // to mock 'onNewSession'
		when(joinPointBeta.getThis()).thenReturn(sessionSlot);
		
		firefoxCaps = new MutableCapabilities();
		firefoxCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.FIREFOX.browserName());
		firefoxCaps.setCapability(CapabilityType.BROWSER_VERSION, "100.0");
		chromeCaps = new MutableCapabilities();
		chromeCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
		chromeCaps.setCapability(CapabilityType.BROWSER_VERSION, "118.0");
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

		
		mockedUtils.when(() -> Utils.getDriverDir()).thenReturn(Paths.get("/home/drivers"));
	}
	
	@AfterMethod(groups = "grid", alwaysRun = true)
	private void reset() {
		mockedUtils.close();
		mockedOSUtility.close();

		// remove properties that could have been set during test
		System.clearProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY);
		System.clearProperty(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY);

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
		when(joinPoint.proceed(new Object[] {createSessionRequest2})).thenReturn(Either.right(activeSession));
		
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
	
	/**
	 * If session cannot be created, refresh browsers
	 * @throws Throwable
	 */
	@Test(groups={"grid"})
	public void testOnNewSessionErrorInProceed2() throws Throwable {
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		doReturn(createSessionRequest2).when(slotActions).beforeStartSession(createSessionRequest, sessionSlot);
		when(joinPoint.proceed(new Object[] {createSessionRequest2})).thenReturn(Either.left(new SessionNotCreatedException("driver cannot support browser version")));
		
		try {
			slotActions.onNewSession(joinPoint);
		} catch (WebDriverException e) {}

		mockedOSUtility.verify(() -> OSUtility.resetInstalledBrowsersWithVersion());
		verify(slotActions, times(2)).beforeStartSession(createSessionRequest, sessionSlot);
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
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			when(nodeStatusClient.isBusyOnOtherSlot(null)).thenReturn(false);

			Map<String, Object> caps = new HashMap<>();
			caps.put(CapabilityType.PLATFORM_NAME, Platform.VISTA);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));

			slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			verify(slotActions).cleanNode();
		}
	}	
	/**
	 * Check list of PIDs is stored before starting the session
	 */
	@Test(groups={"grid"})
	public void testBeforeStartSessionGetPids() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {

			Map<String, Object> caps = new HashMap<>();
			caps.put(CapabilityType.PLATFORM_NAME, Platform.VISTA);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			verify(slotActions).setPreexistingBrowserAndDriverPids(sessionSlot, Arrays.asList(10L, 20L));
		}
	}	
	
	/**
	 * issue #54: Test that when 'platform' is defined with precise OS, for desktop tests, we change platform and platformName capabilities 
	 * Here, Windows 7 (Vista) => Windows
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test(groups={"grid"})
	public void testOnNewSessionUpdatePlatformWindow7Caps() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {

			Map<String, Object> caps = new HashMap<>();
			caps.put(CapabilityType.PLATFORM_NAME, Platform.VISTA);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
			Assert.assertEquals(newSessionRequest.getDesiredCapabilities().getCapability(CapabilityType.PLATFORM_NAME), Platform.WINDOWS);
		}
	}	
	
	/**
	 * issue #54: Test that when 'platform' is defined with general OS, for desktop tests, we do not change platform and platformName capabilities 
	 * Here, Windows  => Windows
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test(groups={"grid"})
	public void testOnNewSessionUpdatePlatformWindowCaps() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> caps = new HashMap<>();
			caps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
			Assert.assertEquals(newSessionRequest.getDesiredCapabilities().getCapability(CapabilityType.PLATFORM_NAME), Platform.WINDOWS);
		}
	}	
	
	/**
	 * Test that chrome driver path is added to session capabilities
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testChromeDriverAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			Map<String, Object> chromeOptions = new HashMap<>();
			chromeOptions.put("binary", "/home/chrome");
			chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
			when(sessionSlot.getStereotype()).thenReturn(chromeCaps);

			createChromeBrowserInfos();

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			// issue #60: check binary is also there
			Assert.assertEquals(((Map<String, Object>) newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("binary"), "/home/chrome");

			// check chrome driver path has been set into property
			Assert.assertTrue(System.getProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).contains("/home/drivers/chromedriver_"));
		}
	}
	
	/**
	 * If no chrome corresponds to requested caps (here beta version), raise exception
	 * This should not happen as when selenium grid creates the session, slot has been matched
	 */
	@Test(groups={"grid"}, expectedExceptions = SessionNotCreatedException.class, expectedExceptionsMessageRegExp = ".*No chrome browser / driver supports requested caps.*")
	public void testChromeDriverNotAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			Map<String, Object> chromeOptions = new HashMap<>();
			chromeOptions.put("binary", "/home/chrome");
			chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
			when(sessionSlot.getStereotype()).thenReturn(chromeCaps);

			createChromeBrowserInfos();

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		}
	}
	
	@Test(groups={"grid"})
	public void testChromeDefaultProfileAdded() {

		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
			((Map<String, List<String>>) requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
			requestedCaps.put(SeleniumRobotCapabilityType.CHROME_PROFILE, "default");
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			createChromeBrowserInfos();

			Map<String, Object> chromeOptions = new HashMap<>();
			chromeOptions.put("binary", "/home/chrome");
			chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
			chromeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/chrome/profile");
			when(sessionSlot.getStereotype()).thenReturn(chromeCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			Assert.assertTrue(((Map<String, List<String>>) newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/chrome/profile"));
		}
	}
	
	@Test(groups={"grid"})
	public void testChromeUserProfileAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
			((Map<String, List<String>>) requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
			requestedCaps.put(SeleniumRobotCapabilityType.CHROME_PROFILE, "/home/user/myprofile");
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			createChromeBrowserInfos();

			Map<String, Object> chromeOptions = new HashMap<>();
			chromeOptions.put("binary", "/home/chrome");
			chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
			chromeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/chrome/profile");
			when(sessionSlot.getStereotype()).thenReturn(chromeCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			Assert.assertTrue(((Map<String, List<String>>) newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/user/myprofile"));
		}
	}
	
	@Test(groups={"grid"})
	public void testChromeNoUserProfileAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.CHROME.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, String>());
			((Map<String, List<String>>) requestedCaps.get(ChromeOptions.CAPABILITY)).put("args", new ArrayList<>());
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			createChromeBrowserInfos();

			Map<String, Object> chromeOptions = new HashMap<>();
			chromeOptions.put("binary", "/home/chrome");
			chromeCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
			chromeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/chrome/profile");
			when(sessionSlot.getStereotype()).thenReturn(chromeCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			Assert.assertEquals(((Map<String, List<String>>) newSessionRequest.getDesiredCapabilities().getCapability(ChromeOptions.CAPABILITY)).get("args").size(), 0);
		}
	}

	private void createChromeBrowserInfos() {
		BrowserInfo chromeInfo = new BrowserInfo(BrowserType.CHROME, "118.0", "/usr/bin/chrome", false, false);
		chromeInfo.setDriverFileName("chromedriver_118");
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.CHROME, Arrays.asList(chromeInfo));
		mockedOSUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion(true)).thenReturn(browserInfos);
	}
	
	private void createEdgeBrowserInfos() {
		BrowserInfo edgeInfo = new BrowserInfo(BrowserType.EDGE, "118.0", "/usr/bin/edge", false, false);
		edgeInfo.setDriverFileName("edgedriver_118");
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.EDGE, Arrays.asList(edgeInfo));
		mockedOSUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion(true)).thenReturn(browserInfos);
	}
	
	private void createFirefoxBrowserInfos() {
		BrowserInfo firefoxInfo = new BrowserInfo(BrowserType.FIREFOX, "118.0", "/usr/bin/firefox", false, false);
		firefoxInfo.setDriverFileName("geckodriver_118");
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		mockedOSUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion(true)).thenReturn(browserInfos);
	}
	
	@Test(groups={"grid"})
	public void testEdgeDriverAdded() {

		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			Map<String, Object> edgeOptions = new HashMap<>();
			edgeOptions.put("binary", "/home/edge");
			edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
			when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

			createEdgeBrowserInfos();

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			// issue #60: check binary is also there
			Assert.assertEquals(((Map<String, Object>) newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("binary"), "/home/edge");

			// check edge driver path has been set into property
			Assert.assertTrue(System.getProperty(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY).contains("/home/drivers/edgedriver_"));
		}
	}
	
	/**
	 * We request a beta browser that the slot cannot provide
	 */
	@Test(groups={"grid"}, expectedExceptions = SessionNotCreatedException.class, expectedExceptionsMessageRegExp = ".*No edge browser / driver supports requested caps.*")
	public void testEdgeDriverNotAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			Map<String, Object> edgeOptions = new HashMap<>();
			edgeOptions.put("binary", "/home/edge");
			edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
			edgeCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, true);
			when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

			createEdgeBrowserInfos();

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
		}
	}
	
	@Test(groups={"grid"})
	public void testEdgeDefaultProfileAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.toString());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(EdgeOptions.CAPABILITY, new HashMap<String, String>());
			((Map<String, List<String>>) requestedCaps.get(EdgeOptions.CAPABILITY)).put("args", new ArrayList<>());
			requestedCaps.put(SeleniumRobotCapabilityType.EDGE_PROFILE, "default");
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			createEdgeBrowserInfos();

			Map<String, Object> edgeOptions = new HashMap<>();
			edgeOptions.put("binary", "/home/edge");
			edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
			edgeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/edge/profile");
			when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			Assert.assertTrue(((Map<String, List<String>>) newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/edge/profile"));
		}
	}
	
	@Test(groups={"grid"})
	public void testEdgeUserProfileAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.toString());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(EdgeOptions.CAPABILITY, new HashMap<String, String>());
			((Map<String, List<String>>) requestedCaps.get(EdgeOptions.CAPABILITY)).put("args", new ArrayList<>());
			requestedCaps.put(SeleniumRobotCapabilityType.EDGE_PROFILE, "/home/user/myprofile");
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			createEdgeBrowserInfos();

			Map<String, Object> edgeOptions = new HashMap<>();
			edgeOptions.put("binary", "/home/edge");
			edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
			edgeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/edge/profile");
			when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			Assert.assertTrue(((Map<String, List<String>>) newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("args").get(0).equals("--user-data-dir=/home/user/myprofile"));
		}
	}
	
	@Test(groups={"grid"})
	public void testEdgeNoUserProfileAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.EDGE.toString());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(EdgeOptions.CAPABILITY, new HashMap<String, String>());
			((Map<String, List<String>>) requestedCaps.get(EdgeOptions.CAPABILITY)).put("args", new ArrayList<>());
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			createEdgeBrowserInfos();

			Map<String, Object> edgeOptions = new HashMap<>();
			edgeOptions.put("binary", "/home/edge");
			edgeCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
			edgeCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/edge/profile");
			when(sessionSlot.getStereotype()).thenReturn(edgeCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			Assert.assertEquals(((Map<String, List<String>>) newSessionRequest.getDesiredCapabilities().getCapability(EdgeOptions.CAPABILITY)).get("args").size(), 0);
		}
	}

	/**
	 * Check profile has been updated ('firefoxProfile' cap is  set to default). Some initial preferences are kept 'general.useragent.override' and 'network.automatic-ntlm-auth.trusted-uris'
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test(groups={"grid"})
	public void testFirefoxDefaultProfileAdded() throws IOException {

		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			FirefoxOptions requestedCaps = new FirefoxOptions();
			requestedCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.FIREFOX.toString());
			requestedCaps.setCapability(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.setProfile(FirefoxProfile.fromJson("{}"));
			requestedCaps.setCapability(SeleniumRobotCapabilityType.FIREFOX_PROFILE, "default");
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(requestedCaps);

			createFirefoxBrowserInfos();

			firefoxCaps.setCapability(FirefoxDriver.SystemProperty.BROWSER_BINARY, "/home/firefox");
			firefoxCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/firefox/profile");
			when(sessionSlot.getStereotype()).thenReturn(firefoxCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			FirefoxProfile newProfile = new FirefoxOptions(newSessionRequest.getDesiredCapabilities()).getProfile();

			// check default preferences
			Assert.assertEquals(newProfile.getStringPreference("general.useragent.override", "no"), "no");
			Assert.assertEquals(newProfile.getStringPreference("network.automatic-ntlm-auth.trusted-uris", "no"), "no");
			Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Window.QueryInterface", ""), SessionSlotActions.ALL_ACCESS);
			Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Window.frameElement.get", ""), SessionSlotActions.ALL_ACCESS);
			Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.HTMLDocument.compatMode.get", ""), SessionSlotActions.ALL_ACCESS);
			Assert.assertEquals(newProfile.getStringPreference("capability.policy.default.Document.compatMode.get", ""), SessionSlotActions.ALL_ACCESS);
			Assert.assertEquals(newProfile.getIntegerPreference("dom.max_chrome_script_run_time", 10), 0);
			Assert.assertEquals(newProfile.getIntegerPreference("dom.max_script_run_time", 10), 0);
		}
	}
	
	/**
	 * Check 'general.useragent.override', 'network.automatic-ntlm-auth.trusted-uris' are kept from provided profile
	 */
	@Test(groups={"grid"})
	public void testFirefoxDefaultProfileAdded2() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
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

			createFirefoxBrowserInfos();

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
	}
	
	/**
	 * Check profile has not been updated ('firefoxProfile' cap is not set)
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test(groups={"grid"})
	public void testFirefoxNoDefaultProfile() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			FirefoxOptions requestedCaps = new FirefoxOptions();
			requestedCaps.setCapability(CapabilityType.BROWSER_NAME, Browser.FIREFOX.toString());
			requestedCaps.setCapability(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			FirefoxProfile profile = new FirefoxProfile();
			profile.setPreference("mypref", "mp");
			requestedCaps.setProfile(profile);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(requestedCaps);

			createFirefoxBrowserInfos();

			firefoxCaps.setCapability(FirefoxDriver.SystemProperty.BROWSER_BINARY, "/home/firefox");
			firefoxCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, "/home/firefox/profile");
			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);
			when(sessionSlot.getStereotype()).thenReturn(firefoxCaps);

			FirefoxProfile newProfile = new FirefoxOptions(newSessionRequest.getDesiredCapabilities()).getProfile();

			// check updated preferences
			Assert.assertEquals(newProfile.getStringPreference("mypref", "no"), "mp");
		}
		
	}
	
	/**
	 * Test that IE driver path is added to session capabilities
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test(groups={"grid"})
	public void testIeDriverAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
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
	}
	
	/**
	 * Check capabilities for testing Edge in IE mode are there
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test(groups={"grid"})
	public void testEdgeIeModeCapabilitiesAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
			Map<String, Object> requestedCaps = new HashMap<>();
			requestedCaps.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
			requestedCaps.put(CapabilityType.PLATFORM_NAME, Platform.WINDOWS);
			requestedCaps.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, true);
			when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(requestedCaps));

			// Edge available
			ieCaps.setCapability(SessionSlotActions.EDGE_PATH, "C:\\msedge.exe");
			when(sessionSlot.getStereotype()).thenReturn(ieCaps);

			CreateSessionRequest newSessionRequest = slotActions.beforeStartSession(createSessionRequest, sessionSlot);

			Assert.assertEquals(((Map<String, Object>) newSessionRequest.getDesiredCapabilities().getCapability(SessionSlotActions.SE_IE_OPTIONS)).get("ie.edgechromium"), true);
			Assert.assertEquals(((Map<String, Object>) newSessionRequest.getDesiredCapabilities().getCapability(SessionSlotActions.SE_IE_OPTIONS)).get("ie.edgepath"), "C:\\msedge.exe");
		}
	}
	
	/**
	 * When Edge in IE mode is requested, but not available, do not add keys
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test(groups={"grid"})
	public void testEdgeIeModeCapabilitiesNotAdded() {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {
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

		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})) {

			slotActions.afterStartSession(new SessionId("2345"), sessionSlot);

			// verify we store only the new PIDS
			verify(slotActions).setCurrentBrowserAndDriverPids(new SessionId("2345"), Arrays.asList(10L, 20L));

			// remove existing pids for this session
			verify(slotActions).removePreexistingPidsForSession(new SessionId("2345"));
		}
	}
	
	@Test(groups={"grid"})
	public void testAfterStartSessionNoExistingPid() {
		when(slotActions.getPreexistingBrowserAndDriverPids(new SessionId("2345"))).thenReturn(null);

		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		})){
			slotActions.afterStartSession(new SessionId("2345"), sessionSlot);

			// verify we store only the new PIDS
			verify(slotActions).setCurrentBrowserAndDriverPids(new SessionId("2345"), new ArrayList<>());
		}
	}
	
	@Test(groups={"grid"})
	public void testBeforeStopSession() throws Exception {
		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
				when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
				when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
				when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
				when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
			});
			MockedConstruction mockedStopVideoCaptureTask = mockConstruction(StopVideoCaptureTask.class)) {

			when(slotActions.getCurrentBrowserAndDriverPids(new SessionId("2345"))).thenReturn(Arrays.asList(10L));

			slotActions.beforeStopSession(new SessionId("2345"), sessionSlot);

			verify((StopVideoCaptureTask)mockedStopVideoCaptureTask.constructed().get(0)).execute();
			verify((DiscoverBrowserAndDriverPidsTask)mockedDiscoverBrowserAndDriverPidsTask.constructed().get(0)).withParentsPids(Arrays.asList(10L)); // we use the current pids (browser and driver) to get the list of all PIDs created by the browser
			verify(slotActions).setPidsToKill(new SessionId("2345"), Arrays.asList(10L, 20L)); // kill process that has been discovered
			verify(slotActions).removeCurrentBrowserPids(new SessionId("2345")); // we have used the current pids, now delete them
		}
	}
	
	/**
	 * Nothing should happen
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testBeforeStopSessionErrorStoppingVideoCapture() throws Exception {

		try (MockedConstruction mockedDiscoverBrowserAndDriverPidsTask = mockConstruction(DiscoverBrowserAndDriverPidsTask.class, (discoverBrowserAndDriverPidsTask, context) -> {
			when(discoverBrowserAndDriverPidsTask.withExistingPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.withParentsPids(anyList())).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.execute()).thenReturn(discoverBrowserAndDriverPidsTask);
			when(discoverBrowserAndDriverPidsTask.getProcessPids()).thenReturn(Arrays.asList(10L, 20L));
		});
		MockedConstruction mockedStopVideoCaptureTask = mockConstruction(StopVideoCaptureTask.class, (stopVideoCaptureTask, context) -> {
			when(stopVideoCaptureTask.execute()).thenThrow(new IOException("file error"));
		});
		) {
			when(slotActions.getCurrentBrowserAndDriverPids(new SessionId("2345"))).thenReturn(Arrays.asList(10L));

			slotActions.beforeStopSession(new SessionId("2345"), sessionSlot);

			verify((StopVideoCaptureTask)mockedStopVideoCaptureTask.constructed().get(0)).execute();
			verify((DiscoverBrowserAndDriverPidsTask)mockedDiscoverBrowserAndDriverPidsTask.constructed().get(0)).withParentsPids(Arrays.asList(10L)); // we use the current pids (browser and driver) to get the list of all PIDs created by the browser
			verify(slotActions).setPidsToKill(new SessionId("2345"), Arrays.asList(10L, 20L)); // kill process that has been discovered
		}
	}
	

	@Test(groups={"grid"})
	public void testAfterStopSession() throws Exception {
		try (MockedConstruction mockedKillTask = mockConstruction(KillTask.class, (killTask, context) -> {
			when(killTask.withPid(anyLong())).thenReturn(killTask);
		})) {
			when(slotActions.getPidsToKill(new SessionId("2345"))).thenReturn(Arrays.asList(10L, 20L));
			when(nodeStatusClient.isBusyOnOtherSlot("2345")).thenReturn(true); // do not clean

			slotActions.afterStopSession(new SessionId("2345"));

			// check pids are killed
			verify((KillTask)mockedKillTask.constructed().get(0)).withPid(10L);
			verify((KillTask)mockedKillTask.constructed().get(1)).withPid(20L);
			verify((KillTask)mockedKillTask.constructed().get(0)).execute();
			verify((KillTask)mockedKillTask.constructed().get(1)).execute();
			verify(slotActions, never()).cleanNode();

		}
	}
	
	/**
	 * Iff error occurs during kill, do not stop
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testAfterStopSessionErrorKilling() throws Exception {
		try (MockedConstruction mockedKillTask = mockConstruction(KillTask.class, (killTask, context) -> {
			when(killTask.withPid(anyLong())).thenReturn(killTask);
			when(killTask.execute()).thenThrow(new IOException("error killing"));
		})) {
			when(slotActions.getPidsToKill(new SessionId("2345"))).thenReturn(Arrays.asList(10L, 20L));
			when(nodeStatusClient.isBusyOnOtherSlot("2345")).thenReturn(true); // do not clean

			slotActions.afterStopSession(new SessionId("2345"));

			// check pids are killed
			verify((KillTask)mockedKillTask.constructed().get(0)).withPid(10L);
			verify((KillTask)mockedKillTask.constructed().get(1)).withPid(20L);
			verify((KillTask)mockedKillTask.constructed().get(0)).execute();
			verify((KillTask)mockedKillTask.constructed().get(1)).execute();
		}
	}
	
	@Test(groups={"grid"})
	public void testAfterStopSessionClean() throws Exception {
		
		when(slotActions.getPidsToKill(new SessionId("2345"))).thenReturn(Arrays.asList(10L, 20L));
		when(nodeStatusClient.isBusyOnOtherSlot("2345")).thenReturn(false); // do not clean
		
		slotActions.afterStopSession(new SessionId("2345"));
		 
		verify(slotActions).cleanNode();
	}
	
	/**
	 * Test that if several thread create a session on the same node, the second thread waits for first thread action terminating (session creation) before going on
	 * @throws Throwable
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 * @throws InterruptedException 
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingSession() throws Throwable {
		
		// simulate a session creation taking 2 secs
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
	
	/**
	 * Test that if several chrome browsers are created at the same time, chrome driver properties are not mixed
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingChromeSession() throws Throwable {
		
		// simulate a session creation taking 2 secs
		Answer<Object> proceedAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				WaitHelper.waitForSeconds(2);
				Assert.assertTrue(System.getProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).startsWith( "/home/drivers/chromedriver_118"));
				return Either.right(activeSession);
			}
		};
		Answer<Object> proceedAnswerBeta = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				WaitHelper.waitForSeconds(2);
				Assert.assertTrue(System.getProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).startsWith("/home/drivers/chromedriver_119"));
				return Either.right(activeSession);
			}
		};
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		
		
		BrowserInfo chromeInfo = new BrowserInfo(BrowserType.CHROME, "118.0", "/usr/bin/chrome", false, false);
		chromeInfo.setDriverFileName("chromedriver_118");
		BrowserInfo chromeInfoBeta = new BrowserInfo(BrowserType.CHROME, "119.0", "/usr/bin/chromeBeta", false, true);
		chromeInfoBeta.setDriverFileName("chromedriver_119");
		
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.CHROME, Arrays.asList(chromeInfo, chromeInfoBeta));

		when(sessionSlot.getStereotype()).thenReturn(chromeCaps);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		DesiredCapabilities betaCaps = new DesiredCapabilities(caps);
		betaCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		when(createSessionRequest2.getDesiredCapabilities()).thenReturn(betaCaps);
		
		when(joinPoint.proceed(any(new Object[] {}.getClass()))).then(proceedAnswer);
		when(joinPointBeta.proceed(any(new Object[] {}.getClass()))).then(proceedAnswerBeta);
		
		Clock clock = Clock.systemUTC();
		Instant start = clock.instant();
		Map<Integer, Boolean> results = Collections.synchronizedMap(new HashMap<>());
		
		SessionSlotActions slotActions2 = spy(new SessionSlotActions(30, nodeStatusClient));
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			try (MockedStatic mockedOSUtility1 = mockStatic(OSUtility.class);
				 MockedStatic mockedUtils1 = mockStatic(Utils.class);
			){
				mockedOSUtility1.when(() -> OSUtility.getInstalledBrowsersWithVersion(true)).thenReturn(browserInfos);
				mockedUtils1.when(() -> Utils.getDriverDir()).thenReturn(Paths.get("/home/drivers"));
				slotActions2.onNewSession(joinPoint);
				results.put(1, true);
			} catch (Throwable e) {
				e.printStackTrace();
				results.put(1, false);
			}
			
		});
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			try (MockedStatic mockedOSUtility2 = mockStatic(OSUtility.class);
				 MockedStatic mockedUtils2 = mockStatic(Utils.class);) {
				mockedOSUtility2.when(() -> OSUtility.getInstalledBrowsersWithVersion(true)).thenReturn(browserInfos);
				mockedUtils2.when(() -> Utils.getDriverDir()).thenReturn(Paths.get("/home/drivers"));
				slotActions2.onNewSession(joinPointBeta);
				results.put(2, true);
			} catch (Throwable e) {
				e.printStackTrace();
				results.put(2, false);
			}
		});
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		
		Assert.assertTrue(results.get(1));
		Assert.assertTrue(results.get(2));	
	}
	
	
	/**
	 * Test that if several chrome browsers are created at the same time, chrome driver properties are not mixed
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingEdgeSession() throws Throwable {
		
		// simulate a session creation taking 2 secs
		Answer<Object> proceedAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				WaitHelper.waitForSeconds(2);
				Assert.assertTrue(System.getProperty(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY).startsWith("/home/drivers/edgedriver_118"));
				return Either.right(activeSession);
			}
		};
		Answer<Object> proceedAnswerBeta = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				WaitHelper.waitForSeconds(2);
				Assert.assertTrue(System.getProperty(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY).startsWith("/home/drivers/edgedriver_119"));
				return Either.right(activeSession);
			}
		};
		
		Map<String, Object> caps = new HashMap<>();
		caps.put("key", "value");
		
		
		BrowserInfo chromeInfo = new BrowserInfo(BrowserType.EDGE, "118.0", "/usr/bin/edge", false, false);
		chromeInfo.setDriverFileName("edgedriver_118");
		BrowserInfo chromeInfoBeta = new BrowserInfo(BrowserType.EDGE, "119.0", "/usr/bin/edgeBeta", false, true);
		chromeInfoBeta.setDriverFileName("edgedriver_119");
		
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.EDGE, Arrays.asList(chromeInfo, chromeInfoBeta));
		
		when(sessionSlot.getStereotype()).thenReturn(edgeCaps);
		when(createSessionRequest.getDesiredCapabilities()).thenReturn(new DesiredCapabilities(caps));
		DesiredCapabilities betaCaps = new DesiredCapabilities(caps);
		betaCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		when(createSessionRequest2.getDesiredCapabilities()).thenReturn(betaCaps);
		
		when(joinPoint.proceed(any(new Object[] {}.getClass()))).then(proceedAnswer);
		when(joinPointBeta.proceed(any(new Object[] {}.getClass()))).then(proceedAnswerBeta);
		
		Clock clock = Clock.systemUTC();
		Instant start = clock.instant();
		Map<Integer, Boolean> results = Collections.synchronizedMap(new HashMap<>());
		
		SessionSlotActions slotActions2 = spy(new SessionSlotActions(30, nodeStatusClient));
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			try (MockedStatic mockedOSUtility1 = mockStatic(OSUtility.class);
				 MockedStatic mockedUtils1 = mockStatic(Utils.class);
			){
				mockedOSUtility1.when(() -> OSUtility.getInstalledBrowsersWithVersion(true)).thenReturn(browserInfos);
				mockedUtils1.when(() -> Utils.getDriverDir()).thenReturn(Paths.get("/home/drivers"));
				slotActions2.onNewSession(joinPoint);
				results.put(1, true);
			} catch (Throwable e) {
				e.printStackTrace();
				results.put(1, false);
			}
			
		});
		executorService.submit(() -> {
			WaitHelper.waitForMilliSeconds(500);
			try (MockedStatic mockedOSUtility2 = mockStatic(OSUtility.class);
				 MockedStatic mockedUtils2 = mockStatic(Utils.class);
			){
				mockedOSUtility2.when(() -> OSUtility.getInstalledBrowsersWithVersion(true)).thenReturn(browserInfos);
				mockedUtils2.when(() -> Utils.getDriverDir()).thenReturn(Paths.get("/home/drivers"));
				slotActions2.onNewSession(joinPointBeta);
				results.put(2, true);
			} catch (Throwable e) {
				e.printStackTrace();
				results.put(2, false);
			}
		});
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		
		Assert.assertTrue(results.get(1));
		Assert.assertTrue(results.get(2));
		
		
	}
	
	/**
	 * Check we do not enter "beforeStartSession" when an other thread is still in
	 * @throws Throwable
	 */
	@Test(groups={"grid"})
	public void concurrencyForCreatingSessionAfterStartSessionNotCalled() throws Throwable {
		
		// simulate a session creation taking 2 secs
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
			// afterStartSession will not be called, so lock won't be released
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
