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
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.infotel.seleniumrobot.grid.servlets.client.NodeClient;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNodeStatus;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openqa.selenium.Platform;
import org.openqa.selenium.edge.EdgeOptions;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.GridStarter;
import com.infotel.seleniumrobot.grid.aspects.SessionSlotActions;
import com.infotel.seleniumrobot.grid.servlets.client.GridStatusClient;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.InstrumentsWrapper;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.logging.SeleniumRobotLogger;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.OSUtilityWindows;

import io.ous.jtoml.JToml;
import io.ous.jtoml.Toml;
import io.ous.jtoml.TomlTable;

@PrepareForTest({AdbWrapper.class, InstrumentsWrapper.class, GridStarter.class, OSUtilityFactory.class, OSUtility.class, LocalAppiumLauncher.class})
public class TestGridStarter extends BaseMockitoTest {
	
	private static Logger logger = SeleniumRobotLogger.getLogger(TestGridStarter.class);
	
	@Mock
	AdbWrapper adbWrapper;
	
	@Mock
	InstrumentsWrapper instrumentsWrapper;
	
	@Mock
	LocalAppiumLauncher appiumLauncher;
	
	@Mock
	OSUtilityWindows osUtility;
	
	@BeforeMethod(groups={"grid"})
	public void init() throws Exception {
		PowerMockito.mockStatic(OSUtilityFactory.class);
		PowerMockito.mockStatic(OSUtility.class);
		PowerMockito.mockStatic(LocalAppiumLauncher.class);
		when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
		when(osUtility.getProgramExtension()).thenReturn("");
		when(OSUtility.getCurrentPlatorm()).thenReturn(Platform.LINUX);
		
		PowerMockito.whenNew(LocalAppiumLauncher.class).withAnyArguments().thenReturn(appiumLauncher);
	}
	
	@Test(groups={"grid"})
	public void testGenerationNoDevices() throws Exception {
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"node"});
		starter.rewriteJsonConf();
		Assert.assertTrue(Arrays.asList(starter.getLaunchConfig().getArgs()).contains("--config"));
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Assert.assertTrue(confFile.contains("generatedNodeConf.toml"));

		// check default values
		Toml conf = JToml.parse(new File(confFile));
		Assert.assertFalse(conf.getBoolean("node", "detect-drivers"));
	}
	
	@Test(groups={"grid"})
	public void testHubGeneration() throws Exception {
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"hub"});
		starter.rewriteJsonConf();
		
		Assert.assertTrue(starter.getLaunchConfig().getArgList().contains("--slot-matcher"));
		Assert.assertTrue(starter.getLaunchConfig().getArgList().contains("com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher"));
	}
	
	@Test(groups={"grid"})
	public void testGenerationMobileDevices() throws Exception {
		
		List<MobileDevice> deviceList = new ArrayList<>();
		deviceList.add(new MobileDevice("IPhone 6", "0000", "ios", "10.2", new ArrayList<>()));
		deviceList.add(new MobileDevice("Nexus 5", "0000", "android", "6.0", Arrays.asList(new BrowserInfo(BrowserType.CHROME, "56.0", null))));
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		PowerMockito.whenNew(InstrumentsWrapper.class).withNoArguments().thenReturn(instrumentsWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(Arrays.asList(deviceList.get(1)));
		when(instrumentsWrapper.parseIosDevices()).thenReturn(Arrays.asList(deviceList.get(0)));
		
		when(appiumLauncher.getAppiumVersion()).thenReturn("1.22.3");
		when(appiumLauncher.getAppiumPort()).thenReturn(5000L);
		
		// no desktop browsers
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(new HashMap<>());
		
		GridStarter starter = new GridStarter(new String[] {"node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Toml conf = JToml.parse(new File(confFile));
		
		Assert.assertEquals(conf.getTomlTable("relay").getString("url"), "http://localhost:5000/wd/hub");
		Assert.assertEquals(conf.getTomlTable("relay").getString("status-endpoint"), "/status");
		Assert.assertEquals(conf.getTomlTable("relay").getArrayTable("configs").size(), 4);

		List<String> configs = (List<String>)conf.getTomlTable("relay").get("configs");
		Assert.assertEquals(configs.get(0), "1"); // max sessions for device 1
		
		JSONObject device1 = new JSONObject(configs.get(1).toString());
		Assert.assertEquals(device1.getString("appium:deviceName"), "Nexus 5");
		Assert.assertEquals(device1.getString("appium:platformVersion"), "6.0"); 
		Assert.assertEquals(device1.getString("platformName"), "ANDROID"); 
		
		Assert.assertEquals(configs.get(2), "1"); // max sessions for device 2
		JSONObject device2 = new JSONObject(configs.get(3).toString());
		Assert.assertEquals(device2.getString("appium:deviceName"), "IPhone 6");
		Assert.assertEquals(device2.getString("appium:platformVersion"), "10.2"); 
		Assert.assertEquals(device2.getString("platformName"), "IOS"); 

	}
	
	/**
	 * With appium2, relay URL is different
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationMobileDevicesAppium2() throws Exception {
		
		List<MobileDevice> deviceList = new ArrayList<>();
		deviceList.add(new MobileDevice("IPhone 6", "0000", "ios", "10.2", new ArrayList<>()));
		deviceList.add(new MobileDevice("Nexus 5", "0000", "android", "6.0", Arrays.asList(new BrowserInfo(BrowserType.CHROME, "56.0", null))));
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		PowerMockito.whenNew(InstrumentsWrapper.class).withNoArguments().thenReturn(instrumentsWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(Arrays.asList(deviceList.get(1)));
		when(instrumentsWrapper.parseIosDevices()).thenReturn(Arrays.asList(deviceList.get(0)));
		
		when(appiumLauncher.getAppiumVersion()).thenReturn("2.0.0");
		when(appiumLauncher.getAppiumPort()).thenReturn(5000L);
		
		// no desktop browsers
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(new HashMap<>());
		
		GridStarter starter = new GridStarter(new String[] {"node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Toml conf = JToml.parse(new File(confFile));
		
		Assert.assertEquals(conf.getTomlTable("relay").getString("url"), "http://localhost:5000");
		
		
	}

	/**
	 * Check node tags a correctly applied
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGenerationMobileDevicesWithTags() throws Exception {
		
		List<MobileDevice> deviceList = new ArrayList<>();
		deviceList.add(new MobileDevice("IPhone 6", "0000", "ios", "10.2", new ArrayList<>()));
		deviceList.add(new MobileDevice("Nexus 5", "0000", "android", "6.0", Arrays.asList(new BrowserInfo(BrowserType.CHROME, "56.0", null))));
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		PowerMockito.whenNew(InstrumentsWrapper.class).withNoArguments().thenReturn(instrumentsWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(Arrays.asList(deviceList.get(1)));
		when(instrumentsWrapper.parseIosDevices()).thenReturn(Arrays.asList(deviceList.get(0)));
		
		when(appiumLauncher.getAppiumVersion()).thenReturn("1.22.3");
		when(appiumLauncher.getAppiumPort()).thenReturn(5000L);
		
		// no desktop browsers
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(new HashMap<>());
		
		GridStarter starter = new GridStarter(new String[] {"node", "--nodeTags", "foo,bar"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Toml conf = JToml.parse(new File(confFile));
		
		Assert.assertEquals(conf.getTomlTable("relay").getString("url"), "http://localhost:5000/wd/hub");
		Assert.assertEquals(conf.getTomlTable("relay").getString("status-endpoint"), "/status");
		Assert.assertEquals(conf.getTomlTable("relay").getArrayTable("configs").size(), 4);
		
		List<String> configs = (List<String>)conf.getTomlTable("relay").get("configs");
		Assert.assertEquals(configs.get(0), "1"); // max sessions for device 1
		
		JSONObject device1 = new JSONObject(configs.get(1).toString());
		Assert.assertEquals(device1.getJSONArray("sr:nodeTags").get(0), "foo");
		Assert.assertEquals(device1.getJSONArray("sr:nodeTags").get(1), "bar");
		
	}
//	
//	@Test(groups={"grid"})
//	public void testNodeTagsMobileDevices() throws Exception {
//		
//		List<MobileDevice> deviceList = new ArrayList<>();
//		deviceList.add(new MobileDevice("IPhone 6", "0000", "ios", "10.2", new ArrayList<>()));
//		deviceList.add(new MobileDevice("Nexus 5", "0000", "android", "6.0", Arrays.asList(new BrowserInfo(BrowserType.CHROME, "56.0", null))));
//		
//		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
//		when(adbWrapper.getDeviceList()).thenReturn(deviceList);
//		
//		// no desktop browsers
//		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(new HashMap<>());
//		
//		GridStarter starter = new GridStarter(new String[] {"-role", "node", "-nodeTags", "foo,bar"});
//		starter.rewriteJsonConf();
//		
//		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
//		
//		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), StandardCharsets.UTF_8));
//		JSONArray configNode = conf.getJSONArray("capabilities");
//	
//		Assert.assertEquals(configNode.length(), 2);
//		Assert.assertEquals(configNode.getJSONObject(0).get("deviceName"), "IPhone 6");
//		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "");
//		Assert.assertEquals(configNode.getJSONObject(0).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(0), "foo");
//		Assert.assertEquals(configNode.getJSONObject(0).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(1), "bar");
//		
//		Assert.assertEquals(configNode.getJSONObject(1).get("deviceName"), "Nexus 5");
//		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "chrome");
//		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(0), "foo");
//		Assert.assertEquals(configNode.getJSONObject(1).getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).getString(1), "bar");
//	}
	
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsers() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "90.0", "/usr/bin/firefox", false, true));
		BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", "/home/iexplore", false, false));
		
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		browsers.put(BrowserType.INTERNET_EXPLORER, Arrays.asList(ieInfo));
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"node", "--max-sessions", "2"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Toml conf = JToml.parse(new File(confFile));
		
		List<TomlTable> driverConfigurations = conf.getArrayTable("node", "driver-configuration");

		Assert.assertEquals(driverConfigurations.size(), 2);
		Assert.assertEquals(driverConfigurations.get(0).getString("display-name"), "firefox 90.0");
		Assert.assertEquals(driverConfigurations.get(0).getLong("max-sessions"), (Long)5L);
		JSONObject firefoxStereotype = new JSONObject(driverConfigurations.get(0).getString("stereotype"));
		
		Assert.assertEquals(firefoxStereotype.getString("browserVersion"), "90.0");
		Assert.assertEquals(firefoxStereotype.getJSONObject("moz:firefoxOptions").getString("binary"), "/usr/bin/firefox");
		Assert.assertEquals(firefoxStereotype.getString("browserName"), "firefox");
		Assert.assertEquals(firefoxStereotype.getString("sr:nodeUrl"), "http://localhost:5555");
		Assert.assertEquals(firefoxStereotype.getInt("sr:maxSessions"), 2);
		Assert.assertTrue((Boolean) firefoxStereotype.getBoolean(SeleniumRobotCapabilityType.BETA_BROWSER));
	
		Assert.assertEquals(driverConfigurations.get(1).getString("display-name"), "internet explorer 11.0");
		Assert.assertEquals(driverConfigurations.get(1).getLong("max-sessions"), (Long)1L);
		JSONObject ieStereotype = new JSONObject(driverConfigurations.get(1).getString("stereotype"));
		
		Assert.assertEquals(ieStereotype.getString("browserVersion"), "11.0");
		Assert.assertEquals(ieStereotype.getString("browserName"), "internet explorer");
		Assert.assertEquals(ieStereotype.getString(SessionSlotActions.EDGE_PATH), "");
		Assert.assertEquals(ieStereotype.getString("sr:nodeUrl"), "http://localhost:5555");
		Assert.assertTrue(driverConfigurations.get(1).getString("webdriver-executable").contains("iedriver"));
		Assert.assertFalse((Boolean) ieStereotype.getBoolean(SeleniumRobotCapabilityType.BETA_BROWSER));
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
		
		GridStarter starter = new GridStarter(new String[] {"node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Toml conf = JToml.parse(new File(confFile));
		
		List<TomlTable> driverConfigurations = conf.getArrayTable("node", "driver-configuration");

		Assert.assertEquals(driverConfigurations.size(), 2);
		Assert.assertEquals(driverConfigurations.get(0).getString("display-name"), "MicrosoftEdge 97.0");
		Assert.assertEquals(driverConfigurations.get(0).getLong("max-sessions"), (Long)5L);
		JSONObject edgeStereotype = new JSONObject(driverConfigurations.get(0).getString("stereotype"));
		
		Assert.assertEquals(edgeStereotype.getString("browserVersion"), "97.0");
		Assert.assertEquals(edgeStereotype.getString("browserName"), "MicrosoftEdge");
		Assert.assertEquals(edgeStereotype.getJSONObject(EdgeOptions.CAPABILITY).getString("binary"), "C:/msedge.exe");
		Assert.assertTrue(driverConfigurations.get(0).getString("webdriver-executable").contains("edgedriver"));
		Assert.assertFalse((Boolean) edgeStereotype.getBoolean(SeleniumRobotCapabilityType.BETA_BROWSER));
	
		Assert.assertEquals(driverConfigurations.get(1).getString("display-name"), "internet explorer 11.0");
		Assert.assertEquals(driverConfigurations.get(1).getLong("max-sessions"), (Long)1L);
		JSONObject ieStereotype = new JSONObject(driverConfigurations.get(1).getString("stereotype"));
		
		Assert.assertEquals(ieStereotype.getString("browserVersion"), "11.0");
		Assert.assertEquals(ieStereotype.getString("browserName"), "internet explorer");
		Assert.assertEquals(ieStereotype.getString(SessionSlotActions.EDGE_PATH), "C:/msedge.exe");
		Assert.assertTrue(driverConfigurations.get(1).getString("webdriver-executable").contains("iedriver"));
		Assert.assertFalse((Boolean) ieStereotype.getBoolean(SeleniumRobotCapabilityType.BETA_BROWSER));
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
		
		GridStarter starter = new GridStarter(new String[] {"node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Toml conf = JToml.parse(new File(confFile));
		
		List<TomlTable> driverConfigurations = conf.getArrayTable("node", "driver-configuration");

		Assert.assertEquals(driverConfigurations.size(), 2);
		Assert.assertEquals(driverConfigurations.get(0).getString("display-name"), "MicrosoftEdge 97.0");
		Assert.assertEquals(driverConfigurations.get(0).getLong("max-sessions"), (Long)5L);
		JSONObject edgeStereotype = new JSONObject(driverConfigurations.get(0).getString("stereotype"));
		
		Assert.assertEquals(edgeStereotype.getString("browserVersion"), "97.0");
		Assert.assertEquals(edgeStereotype.getString("browserName"), "MicrosoftEdge");
		Assert.assertEquals(edgeStereotype.getJSONObject(EdgeOptions.CAPABILITY).getString("binary"), "C:/msedge.exe");
		Assert.assertTrue(driverConfigurations.get(0).getString("webdriver-executable").contains("edgedriver"));
		Assert.assertTrue((Boolean) edgeStereotype.getBoolean(SeleniumRobotCapabilityType.BETA_BROWSER));
	
		Assert.assertEquals(driverConfigurations.get(1).getString("display-name"), "internet explorer 11.0");
		Assert.assertEquals(driverConfigurations.get(1).getLong("max-sessions"), (Long)1L);
		JSONObject ieStereotype = new JSONObject(driverConfigurations.get(1).getString("stereotype"));
		
		Assert.assertEquals(ieStereotype.getString("browserVersion"), "11.0");
		Assert.assertEquals(ieStereotype.getString("browserName"), "internet explorer");
		Assert.assertTrue(driverConfigurations.get(1).getString("webdriver-executable").contains("iedriver"));
		Assert.assertFalse((Boolean) ieStereotype.getBoolean(SeleniumRobotCapabilityType.BETA_BROWSER));
		Assert.assertEquals(ieStereotype.get(SessionSlotActions.EDGE_PATH), ""); // EdgePath is no set as Edge is installed in version beta only
	}
	
	/**
	 * Check nodeTags is added to slot capabilities
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testNodeTagsForGenerationDesktopBrowsers() throws Exception {
		
		Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "90.0", "/usr/bin/firefox", false, true));
		BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", null));
		
		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();
		
		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		browsers.put(BrowserType.INTERNET_EXPLORER, Arrays.asList(ieInfo));
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"node", "--nodeTags", "foo,bar"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		Toml conf = JToml.parse(new File(confFile));
		
		List<TomlTable> driverConfigurations = conf.getArrayTable("node", "driver-configuration");
		JSONObject firefoxStereotype = new JSONObject(driverConfigurations.get(0).getString("stereotype"));
		Assert.assertEquals(firefoxStereotype.getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).length(), 2);
		Assert.assertEquals(firefoxStereotype.getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).get(0), "foo");
		Assert.assertEquals(firefoxStereotype.getJSONArray(SeleniumRobotCapabilityType.NODE_TAGS).get(1), "bar");

	}


	/**
	 * Test that start a node and a hub, and checks that status API do not change
	 * /!\ This does not check the "session" content on node
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testGridStart() throws Exception {


		Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
		BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "90.0", "/usr/bin/firefox", false, true));
		BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", "/home/iexplore", false, false));

		Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
		Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();

		browsers.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));
		browsers.put(BrowserType.INTERNET_EXPLORER, Arrays.asList(ieInfo));
		when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);

		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());


		StartGridThread gridHubThread = new StartGridThread(new String[] {"hub"});
		gridHubThread.start();


		GridStatusClient gridStatusClient = new GridStatusClient(new URL(String.format("http://localhost:%d", gridHubThread.getPort())));
		// wait for hub to be up
		boolean started = false;
		for (int i=0; i < 10; i++) {
			try {
				gridStatusClient.getStatus();
				started = true;
				break;
			} catch (Exception e) {
				logger.info("Hub not started, wait");
				WaitHelper.waitForSeconds(5);
			}
		}
		Assert.assertTrue(started, "Hub never started");

		Assert.assertFalse(gridStatusClient.isReady()); // no connected nodes

		StartGridThread gridNodeThread = new StartGridThread(new String[] {"node", "--max-sessions", "3", "--override-max-sessions", "true", "--hub", String.format("http://localhost:%d", gridHubThread.getPort())});
		gridNodeThread.start();

		// wait for grid to be ready
		started = false;
		for (int i=0; i < 20; i++) {
			if (gridStatusClient.isReady()) {
				started = true;
				break;
			} else {

				logger.info("Node not started, wait");
				WaitHelper.waitForSeconds(5);
			}

		}
		Assert.assertTrue(started, "Node never started");

		NodeClient nodeClient = new NodeClient(new URL("http://localhost:5555"));
		Assert.assertTrue(nodeClient.isReady());
		SeleniumNodeStatus nodeStatus = nodeClient.getStatus();
		Assert.assertTrue(nodeStatus.isReady());
		Assert.assertEquals(nodeStatus.getTestSlots(), 6);
		Assert.assertEquals(nodeStatus.getSessionList().size(), 0);
	}
	
	class StartGridThread extends Thread {
		
		private GridStarter starter;
		private int port;
		private String[] args;
		
		public StartGridThread(String[] args) {
			this.args = args;
			this.port = findFreePort();
		}
		

		private int findFreePort() {
			ServerSocket socket = null;
			try {
				socket = new ServerSocket(0);
				socket.setReuseAddress(true);
				int port = socket.getLocalPort();
				
				return port;
			} catch (IOException e) { 
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
					}
				}
			}
			throw new IllegalStateException("Could not find a free TCP/IP port ");
		}
		
		public void run() {
			logger.info("start");
			try {
				List<String> newArgs = new ArrayList<>(Arrays.asList(args));
				if (args[0].equals("hub")) {
					newArgs.add("--port");
					newArgs.add(Integer.toString(port));
					newArgs.add("--host");
					newArgs.add("127.0.0.1");
				}
				starter = new GridStarter(newArgs.toArray(new String[] {}));
				starter.configure();
				starter.start(starter.getLaunchConfig().getArgs());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			logger.info("stop");
		}


		public GridStarter getStarter() {
			return starter;
		}


		public int getPort() {
			return port;
		}
	}
}