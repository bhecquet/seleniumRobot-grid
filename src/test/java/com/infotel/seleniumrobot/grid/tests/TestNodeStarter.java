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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.GridStarter;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.OSUtilityWindows;

@PrepareForTest({AdbWrapper.class, GridStarter.class, OSUtilityFactory.class})
public class TestNodeStarter extends BaseMockitoTest {
	
	@Mock
	AdbWrapper adbWrapper;
	
	@Mock
	OSUtilityWindows osUtility;
	
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
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), Charset.forName("UTF-8")));
		JSONObject configNode = conf.getJSONObject("configuration");
		
		// check default values
		Assert.assertEquals(configNode.getInt("maxSession"), 10);
		Assert.assertEquals(configNode.getInt("port"), 5555);
		Assert.assertEquals(configNode.getString("host"), "ip");
		Assert.assertEquals(configNode.getInt("hubPort"), 4444);
		Assert.assertEquals(configNode.getString("hubHost"), "ip");
	}
	
	@Test(groups={"grid"})
	public void testGenerationMobileDevices() throws Exception {
		
		List<MobileDevice> deviceList = new ArrayList<>();
		deviceList.add(new MobileDevice("IPhone 6", "0000", "ios", "10.2", new ArrayList<>()));
		
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(deviceList);
		
		// no desktop browsers
		PowerMockito.mockStatic(OSUtilityFactory.class);
		when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
		when(osUtility.getInstalledBrowsers()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), Charset.forName("UTF-8")));
		JSONArray configNode = conf.getJSONArray("capabilities");
	
		Assert.assertEquals(configNode.length(), 1);
		Assert.assertEquals(configNode.getJSONObject(0).get("deviceName"), "IPhone 6");
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "");
	}
	
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsers() throws Exception {
		
		List<BrowserType> browsers = new ArrayList<>();
		browsers.add(BrowserType.FIREFOX);
		browsers.add(BrowserType.INTERNET_EXPLORER);

		PowerMockito.mockStatic(OSUtilityFactory.class);
		when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
		when(osUtility.getInstalledBrowsers()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), Charset.forName("UTF-8")));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 2);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "internet explorer");
		Assert.assertEquals(configNode.getJSONObject(1).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 5);
	}
	
	@Test(groups={"grid"})
	public void testGenerationDesktopBrowsersFromArgs() throws Exception {
		
		List<BrowserType> browsers = new ArrayList<>();
		browsers.add(BrowserType.FIREFOX);
		
		PowerMockito.mockStatic(OSUtilityFactory.class);
		when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
		when(osUtility.getInstalledBrowsers()).thenReturn(browsers);
		
		// no mobile devices
		PowerMockito.whenNew(AdbWrapper.class).withNoArguments().thenReturn(adbWrapper);
		when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
		
		GridStarter starter = new GridStarter(new String[] {"-role", "node", 
									"-browser", 
									"browserName=firefox,version=3.6,firefox_binary=/home/myhomedir/firefox36/firefox,maxInstances=3,platform=LINUX", 
									"-browser",
									"browserName=firefox,version=4,firefox_binary=/home/myhomedir/firefox4/firefox,maxInstances=4,platform=LINUX"});
		starter.rewriteJsonConf();
		
		String confFile = starter.getLaunchConfig().getArgs()[starter.getLaunchConfig().getArgs().length - 1];
		
		JSONObject conf = new JSONObject(FileUtils.readFileToString(new File(confFile), Charset.forName("UTF-8")));
		JSONArray configNode = conf.getJSONArray("capabilities");
		
		Assert.assertEquals(configNode.length(), 3);
		Assert.assertEquals(configNode.getJSONObject(0).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(0).get("seleniumProtocol"), "WebDriver");
		Assert.assertEquals(configNode.getJSONObject(0).get("maxInstances"), 5);
		Assert.assertEquals(configNode.getJSONObject(1).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(1).get("version"), "3.6");
		Assert.assertEquals(configNode.getJSONObject(1).get("maxInstances"), 3);
		Assert.assertEquals(configNode.getJSONObject(1).get("platform"), "LINUX");
		Assert.assertEquals(configNode.getJSONObject(2).get("browserName"), "firefox");
		Assert.assertEquals(configNode.getJSONObject(2).get("version"), "4");
		Assert.assertEquals(configNode.getJSONObject(2).get("maxInstances"), 4);
		Assert.assertEquals(configNode.getJSONObject(2).get("platform"), "LINUX");
	}
	
}
