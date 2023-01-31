/**
 * Copyright 2017 www.infotel.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infotel.seleniumrobot.grid.tests;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Platform;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.CustomRemoteProxy;
import com.infotel.seleniumrobot.grid.GridStarter;
import com.infotel.seleniumrobot.grid.utils.CommandLineOptionHelper;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.OSUtilityWindows;

@PrepareForTest({AdbWrapper.class, GridStarter.class, OSUtilityFactory.class, OSUtility.class})
public class TestGridStarter extends BaseMockitoTest {
	
	@Mock
	AdbWrapper adbWrapper;
	
	@Mock
	OSUtilityWindows osUtility;
	
	@BeforeMethod(groups={"grid"})
	public void init() {
		PowerMockito.mockStatic(OSUtilityFactory.class);
		PowerMockito.mockStatic(OSUtility.class);
		when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
		when(osUtility.getProgramExtension()).thenReturn("");
		when(OSUtility.getCurrentPlatorm()).thenReturn(Platform.LINUX);
	}
	
	@Test(groups={"grid"})
	public void testNoGenerationWhenNodeconfigSet() throws IOException {
		GridStarter starter = new GridStarter(new String[] {"-role", "node", "-nodeConfig", "conf.json"});
		starter.rewriteJsonConf();
		Assert.assertTrue(Arrays.asList(starter.getLaunchConfig().getArgs()).contains("-nodeConfig"));
		Assert.assertTrue(Arrays.asList(starter.getLaunchConfig().getArgs()).contains("conf.json"));
	}
	
	@Test(groups={"grid"})
	public void testNoGenerationWhenHubRole() throws IOException {
		GridStarter starter = new GridStarter(new String[] {"-role", "hub", "-hubConfig", "conf.json"});
		starter.rewriteJsonConf();
		Assert.assertFalse(Arrays.asList(starter.getLaunchConfig().getArgs()).contains("-nodeConfig"));
		Assert.assertTrue(Arrays.asList(starter.getLaunchConfig().getArgs()).contains("conf.json"));
	}
	
	@Test(groups={"grid"})
	public void testGenerationNoDevices() throws Exception {
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node"});
		starter.rewriteJsonConf();
		Assert.assertTrue(Arrays.asList(starter.getLaunchConfig().getArgs()).contains("-nodeConfig"));
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Assert.assertTrue(confFile.contains("generatedNodeConf.json"));
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		
		// check default values
		Assert.assertEquals(conf.getInt("maxSession"), 5);
		Assert.assertEquals(conf.getInt("port"), -1); // since selenium 3.12.0, default port is -1 (assigned at runtime)
		Assert.assertEquals(conf.getString("hub"), "http://localhost:4444");
		Assert.assertEquals(conf.getString("proxy"), "com.infotel.seleniumrobot.grid.CustomRemoteProxy");
		Assert.assertEquals(conf.getJSONArray("servlets").toList().size(), 4);
	}
	
	@Test(groups={"grid"})
	public void testHubGeneration() throws Exception {
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "hub"});
		starter.rewriteJsonConf();
		Assert.assertTrue(Arrays.asList(starter.getLaunchConfig().getArgs()).contains("-hubConfig"));
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Assert.assertTrue(confFile.contains("generatedHubConf.json"));
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		
		// check default values
		Assert.assertEquals(conf.getString("role"), "hub");
		Assert.assertEquals(conf.getInt("port"), 4444);
		Assert.assertEquals(conf.getString("capabilityMatcher"), "com.infotel.seleniumrobot.grid.CustomCapabilityMatcher");
		
		// dur to issue https://github.com/SeleniumHQ/selenium/issues/6188, this check is temporary disabled and replaced by the next lines (check of arguments)
//		Assert.assertEquals(conf.getJSONArray("servlets").toList().size(), 2);
//		Assert.assertEquals(conf.getJSONArray("servlets").get(0), "com.infotel.seleniumrobot.grid.servlets.server.GuiServlet");
		
		List<String> servlets = new CommandLineOptionHelper(starter.getLaunchConfig().getArgList()).getAll("-servlet");
		Assert.assertEquals(servlets.size(), 4);
		Assert.assertEquals(servlets.get(0), "com.infotel.seleniumrobot.grid.servlets.server.GuiServlet");
	}
	
	@Test(groups={"grid"})
	public void testGenerationMobileDevices() throws Exception {
		
		List<MobileDevice> deviceList = new ArrayList<>();
		deviceList.add(new MobileDevice("IPhone 6", "0000", "ios", "10.2", new ArrayList<>()));
		deviceList.add(new MobileDevice("Nexus 5", "0000", "android", "6.0", Arrays.asList(new BrowserInfo(BrowserType.CHROME, "56.0", null))));
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(deviceList);
		
		// no desktop browsers
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(new HashMap<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
	
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("deviceName"), "IPhone 6");
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "");
		Assert.assertEquals(configNode.getJSONObject(1).get("deviceName"), "Nexus 5");
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "chrome");
	}
	
	@Test(groups={"grid"})
	public void testNodeTagsMobileDevices() throws Exception {
		
		List<MobileDevice> deviceList = new ArrayList<>();
		deviceList.add(new MobileDevice("IPhone 6", "0000", "ios", "10.2", new ArrayList<>()));
		deviceList.add(new MobileDevice("Nexus 5", "0000", "android", "6.0", Arrays.asList(new BrowserInfo(BrowserType.CHROME, "56.0", null))));
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(deviceList);
		
		// no desktop browsers
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(new HashMap<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", "-nodeTags", "foo,bar"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
	
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("deviceName"), "IPhone 6");
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "");
		Assert.assertEquals(configNode.getJSONObject(0).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(0), "foo");
		Assert.assertEquals(configNode.getJSONObject(0).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(1), "bar");
		
		Assert.assertEquals(configNode.getJSONObject(1).get("deviceName"), "Nexus 5");
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "chrome");
		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(0), "foo");
		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(1), "bar");
	}
	
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsers() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "90.0", null, false, true));
		BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", null));
		
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		browsers.put(BrowserType.INTERNET_EXPLORER, Arrays.asList(ieInfo));
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("browserVersion"), "90.0");
		Assert.assertTrue((Boolean) configNode.getJSONObject(0).get("beta"));
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertTrue(configNode.getJSONObject(0).getString("webdriver.gecko.driver").contains("geckodriver"));
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "internet explorer");
		Assert.assertTrue(configNode.getJSONObject(1).getString("webdriver.ie.driver").contains("iedriver"));
		Assert.assertEquals(configNode.getJSONObject(1).get("browserVersion"), "11.0");
		Assert.assertFalse((Boolean) configNode.getJSONObject(1).get("beta"));
		Assert.assertEquals(configNode.getJSONObject(1).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 1);
		Assert.assertFalse(configNode.getJSONObject(1).has(CustomRemoteProxy.EDGE_PATH)); // EdgePath is no set as Edge is not installed 
	}
	

	/**
	 * Check that if Edge is installed, edgePath capability is set
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersEdgeIeMode() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
		BrowserInfo edgeInfo = Mockito.spy(new BrowserInfo(BrowserType.EDGE, "97.0", "C:\\msedge.exe", false, false));
		BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", null));
		
		Mockito.doReturn("edgedriver").when(edgeInfo).getDriverFileName();
		Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();
		
		browsers.put(BrowserType.EDGE, Arrays.asList(edgeInfo));
		browsers.put(BrowserType.INTERNET_EXPLORER, Arrays.asList(ieInfo));
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), org.openqa.selenium.remote.BrowserType.EDGE);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserVersion"), "97.0");
		Assert.assertFalse((Boolean) configNode.getJSONObject(0).get("beta"));
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertTrue(configNode.getJSONObject(0).getString("webdriver.edge.driver").contains("edgedriver"));
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(0).get("edge_binary"), "C:\\msedge.exe");
		
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), org.openqa.selenium.remote.BrowserType.IE);
		Assert.assertTrue(configNode.getJSONObject(1).getString("webdriver.ie.driver").contains("iedriver"));
		Assert.assertEquals(configNode.getJSONObject(1).get("browserVersion"), "11.0");
		Assert.assertFalse((Boolean) configNode.getJSONObject(1).get("beta"));
		Assert.assertEquals(configNode.getJSONObject(1).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 1);
		Assert.assertEquals(configNode.getJSONObject(1).get(CustomRemoteProxy.EDGE_PATH), "C:\\msedge.exe"); // EdgePath is set as Edge is installed
	}

	/**
	 * Check that if Edge is installed only in beta, edgePath capability is not set at all
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersEdgeIeModeBeta() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
		BrowserInfo edgeInfo = Mockito.spy(new BrowserInfo(BrowserType.EDGE, "97.0", "C:\\msedge.exe", false, true));
		BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", null));
		
		Mockito.doReturn("edgedriver").when(edgeInfo).getDriverFileName();
		Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();
		
		browsers.put(BrowserType.EDGE, Arrays.asList(edgeInfo));
		browsers.put(BrowserType.INTERNET_EXPLORER, Arrays.asList(ieInfo));
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), org.openqa.selenium.remote.BrowserType.EDGE);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserVersion"), "97.0");
		Assert.assertTrue((Boolean) configNode.getJSONObject(0).get("beta"));
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertTrue(configNode.getJSONObject(0).getString("webdriver.edge.driver").contains("edgedriver"));
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(0).get("edge_binary"), "C:\\msedge.exe");
		
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), org.openqa.selenium.remote.BrowserType.IE);
		Assert.assertTrue(configNode.getJSONObject(1).getString("webdriver.ie.driver").contains("iedriver"));
		Assert.assertEquals(configNode.getJSONObject(1).get("browserVersion"), "11.0");
		Assert.assertFalse((Boolean) configNode.getJSONObject(1).get("beta"));
		Assert.assertEquals(configNode.getJSONObject(1).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 1);
		Assert.assertFalse(configNode.getJSONObject(1).has(CustomRemoteProxy.EDGE_PATH)); // EdgePath is no set as Edge is installed in version beta only
	}
	
	@Test(groups={"grid"})
	public void testNodeTagsForGenerationDesktopBrowsers() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", null));
		
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		browsers.put(BrowserType.INTERNET_EXPLORER, Arrays.asList(ieInfo));
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", "-nodeTags", "foo,bar"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(0), "foo");
		Assert.assertEquals(configNode.getJSONObject(0).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(1), "bar");
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "internet explorer");
		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(0), "foo");
		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(1), "bar");
	}
	
	/**
	 * Test that browser passed to the command line are added to the browser list automatically found by grid
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersFromArgs() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
									"-browser", 
									"browserName=firefox,version=3.6,firefox_binary=/home/myhomedir/firefox36/firefox,maxInstances=3,platform=LINUX,webdriver.gecko.driver=geckodriver", 
									"-browser",
									"browserName=firefox,version=4,firefox_binary=/home/myhomedir/firefox4/firefox,maxInstances=4,platform=LINUX,webdriver.gecko.driver=geckodriver"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 3);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(1).get("version"), "3.6");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 3);
		Assert.assertEquals(configNode.getJSONObject(1).get("platform"), "LINUX");
		Assert.assertEquals(configNode.getJSONObject(1).get("webdriver.gecko.driver"), "geckodriver");
		Assert.assertEquals(configNode.getJSONObject(2).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(2).get("version"), "4");
		Assert.assertEquals(configNode.getJSONObject(2).get("maxInstances"), 4);
		Assert.assertEquals(configNode.getJSONObject(2).get("platform"), "LINUX");
		Assert.assertEquals(configNode.getJSONObject(2).get("webdriver.gecko.driver"), "geckodriver");
	}
	
	/**
	 * Test that browser passed to the command line are added to the browser list automatically found by grid
	 * - in this test, we check that it's possible to only provide browserName, binary and version for chrome
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersFromArgsAutoChromeParams() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-browser", 
		"browserName=chrome,version=109.0,chrome_binary=/home/myhomedir/chrome99/chrome"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "chrome");
		Assert.assertEquals(configNode.getJSONObject(1).get("version"), "109.0");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 5); // check maxInstances has been filled automatically
		Assert.assertNotNull(configNode.getJSONObject(1).get("platform")); // check platform has been filled automatically
		Assert.assertEquals(configNode.getJSONObject(1).get("chrome_binary"), "/home/myhomedir/chrome99/chrome");
		Assert.assertTrue(configNode.getJSONObject(1).getString("webdriver.chrome.driver").endsWith("drivers/chromedriver_109.0_chrome-109-110"));
	}
	
	@Test(groups={"grid"}, expectedExceptions = GridException.class)
	public void testGenerationDesktopBrowsersFromArgsAutoNoBinary() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-browser", 
		"browserName=chrome,version=83.0"});
		starter.rewriteJsonConf();
	}
	
	@Test(groups={"grid"}, expectedExceptions = GridException.class)
	public void testGenerationDesktopBrowsersFromArgsNoVersion() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-browser", 
		"browserName=chrome,chrome_binary=/home/myhomedir/chrome83/chrome"});
		starter.rewriteJsonConf();
		
	}
	
	@Test(groups={"grid"}, expectedExceptions = GridException.class)
	public void testGenerationDesktopBrowsersFromArgsNoName() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-browser", 
		"browserNames=chrome,version=83.0,chrome_binary=/home/myhomedir/chrome83/chrome"});
		starter.rewriteJsonConf();
		
	}
	
	/**
	 * Test that browser passed to the command line are added to the browser list automatically found by grid
	 * - in this test, we check that it's possible to only provide browserName, binary and version for firefox
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersFromArgsAutoFirefoxParams() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-browser", 
				"browserName=firefox,version=70.0,firefox_binary=/home/myhomedir/firefox70/firefox"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(1).get("version"), "70.0");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("firefox_binary"), "/home/myhomedir/firefox70/firefox");
		Assert.assertTrue(configNode.getJSONObject(1).getString("webdriver.gecko.driver").endsWith("drivers/geckodriver"));
	}

	@Test(groups={"grid"}, expectedExceptions = GridException.class)
	public void testGenerationDesktopBrowsersFromArgsAutoNoFFBinary() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-browser", 
		"browserName=firefox,version=83.0"});
		starter.rewriteJsonConf();
	}
	
	/**
	 * Test that browser passed to the command line are added to the browser list automatically found by grid
	 * - in this test, we check that it's possible to only provide browserName, binary and version for IE
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersFromArgsAutoIEParams() throws Exception {
		

		when(OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);
		when(osUtility.getProgramExtension()).thenReturn(".exe");
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-browser", 
		"browserName=internet explorer,version=11"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "internet explorer");
		Assert.assertEquals(configNode.getJSONObject(1).get("version"), "11");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 1);
		Assert.assertNull(configNode.getJSONObject(1).opt("firefox_binary"));
		Assert.assertNull(configNode.getJSONObject(1).opt("chrome_binary"));
		Assert.assertTrue(configNode.getJSONObject(1).getString("webdriver.ie.driver").endsWith("drivers/IEDriverServer_Win32.exe"));
	}

	/**
	 * Test that browser passed to the command line are added to the browser list automatically found by grid
	 * - in this test, we check that nodeTags is added to parameters
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersFromArgsNodeTags() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "56.0", null));
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
				"-restrictToTags", "true",
				"-nodeTags", "foo",
				"-browser", 
				"browserName=firefox,version=70.0,firefox_binary=/home/myhomedir/firefox70/firefox"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray("nodeTags").length(), 1);
		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray("nodeTags").get(0), "foo");
		Assert.assertEquals(configNode.getJSONObject(1).get("restrictToTags"), true);
	}
	
}
