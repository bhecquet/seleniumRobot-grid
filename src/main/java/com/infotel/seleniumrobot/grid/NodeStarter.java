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
package com.infotel.seleniumrobot.grid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.common.CommandLineOptionHelper;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.selenium.GridLauncher;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;

import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.osutility.OSUtilityFactory;

import io.appium.java_client.remote.MobileCapabilityType;

public class NodeStarter {
	
	private static final Logger logger = Logger.getLogger(NodeStarter.class.getName());

	private String[] args;

    public NodeStarter(String[] args) {
        this.args = args;
    }

    public String[] getArgs() {
		return args;
	}

	public static void main(String[] args) throws Exception {

        NodeStarter starter = new NodeStarter(args);
        starter.configure();
        starter.start();
    }

    
    /**
     * Adds driver paths to configuration
     * @param nodeConf
     */
    private void addDriverToConfiguration(JSONObject nodeConf) {
    	
    	String ext = OSUtilityFactory.getInstance().getProgramExtension();
		String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/");
		String platformName = Platform.getCurrent().family().toString().toLowerCase();
		
    	
    	JSONObject configNode = nodeConf.getJSONObject("configuration");
		configNode.put(String.format("Dwebdriver.chrome.driver=%s/chromedriver%s", driverPath, ext), "");
		configNode.put(String.format("Dwebdriver.gecko.driver=%s/geckodriver%s", driverPath, ext), "");
		
		if ("windows".equals(platformName)) {
			configNode.put(String.format("Dwebdriver.edge.driver=%s/MicrosoftWebDriver%s", driverPath, ext), "");
			configNode.put(String.format("Dwebdriver.ie.driver=%s/IEDriverServer%s", driverPath, ext), "");
		}
    }
    
    private void addMobileDevicesToConfiguration(JSONObject nodeConf) {
    	
    	JSONArray configNode = nodeConf.getJSONArray("capabilities");
    	
    	// handle android devices
    	try {
    		AdbWrapper adb = new AdbWrapper();
    		
    		for (MobileDevice device: adb.getDeviceList()) {
    			JSONObject jsonDevice = new JSONObject();
    			jsonDevice.put(MobileCapabilityType.PLATFORM_NAME, "android");
    			jsonDevice.put(MobileCapabilityType.PLATFORM_VERSION, device.getVersion());
    			jsonDevice.put(MobileCapabilityType.DEVICE_NAME, device.getName());
    			jsonDevice.put(CapabilityType.BROWSER_NAME, StringUtils.join(device.getBrowsers(), ","));
    			jsonDevice.put("maxInstances", 1);
    			configNode.put(jsonDevice);
    		}
    		
    	} catch (ConfigurationException e) {
    		// do nothing as ADB may not be installed on a node
    	}
    	
    	
    }
    
    /**
     * reads the default configuration in resources and replaces variable values
     * Default values will be used only if no value has been overriden by user
     * @return
     */
    private JSONObject createDefaultConfiguration() {
    	InputStream confStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("templates/nodeConf.json");
    	try {
			// if any value is declared in args, it will be overriden by GridLauncher
			return new JSONObject(IOUtils.toString(confStream));
		} catch (JSONException | IOException e) {
			throw new GridException("Cannot read configuration template", e);
		}
    }
    
    /**
     * Add browser from user parameters
     * @param gridConf
     */
    private void addDesktopBrowsersToConfiguration(JSONObject gridConf) {
    	
    	JSONArray configNode = gridConf.getJSONArray("capabilities");
    	
    	for (BrowserType browser: OSUtilityFactory.getInstance().getInstalledBrowsers()) {
    		String gridType;
    		try {
    			Field browField = org.openqa.selenium.remote.BrowserType.class.getDeclaredField(browser.name());
    			gridType = (String)browField.get(org.openqa.selenium.remote.BrowserType.class);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				if (browser == BrowserType.INTERNET_EXPLORER) {
					gridType = org.openqa.selenium.remote.BrowserType.IE;
				} else if (browser == BrowserType.BROWSER) {
					gridType = BrowserType.BROWSER.toString();
				} else {
					continue;
				}
			}
    		
    		JSONObject jsonDevice = new JSONObject();
			jsonDevice.put(CapabilityType.BROWSER_NAME, gridType);
			jsonDevice.put("seleniumProtocol", "WebDriver");
			jsonDevice.put("maxInstances", 5);
			jsonDevice.put(CapabilityType.PLATFORM, Platform.getCurrent());
			configNode.put(jsonDevice);
    	}
    }
    
    private void addBrowsersFromArguments(JSONObject gridConf) {
    	
    	CommandLineOptionHelper helper = new CommandLineOptionHelper(args);
    	if (helper.isParamPresent("-browser")) {
    		JSONArray configNode = gridConf.getJSONArray("capabilities");
    		for (String browserConf: helper.getAll("-browser")) {
    			JSONObject jsonDevice = new JSONObject();
    			for (String pair: browserConf.split(",")) {
    				String[] keyValue = pair.split("=", 2);
    				if ("maxInstances".equals(keyValue[0])) {
    					jsonDevice.put(keyValue[0], Integer.parseInt(keyValue[1]));
    				} else {
    					jsonDevice.put(keyValue[0], keyValue[1]);
    				}
    			}
    			configNode.put(jsonDevice);
    		}
    	}
    }
    
    /**
     * Method for generating json configuration in case none has been specified
     */
    public void rewriteJsonConf() {
    	CommandLineOptionHelper helper = new CommandLineOptionHelper(args);
    	if (!helper.isParamPresent("-nodeConfig") && helper.isParamPresent("-role") && helper.getParamValue("-role").equals("node")) {
    		
    		try {
    			
				JSONObject nodeConf = createDefaultConfiguration();
				addDriverToConfiguration(nodeConf);
				addMobileDevicesToConfiguration(nodeConf);
				addDesktopBrowsersToConfiguration(nodeConf);
				addBrowsersFromArguments(nodeConf);
				
				File newConfFile = Paths.get(Utils.getRootdir(), "generatedNodeConf.json").toFile();
				FileUtils.writeStringToFile(newConfFile, nodeConf.toString(4));
				
				// rewrite args with new configuration
				List<String> newArgs = new ArrayList<>();
				newArgs.addAll(Arrays.asList(args));
				newArgs.add("-nodeConfig");
				newArgs.add(newConfFile.getAbsolutePath());
				args = newArgs.toArray(new String[0]);
				
			} catch (IOException e) {
				throw new GridException("Cannot generate conf file ", e);
			}
    	}
    }
    
    private void killExistingDrivers() {
    	OSUtilityFactory.getInstance().killAllWebDriverProcess();
    }
    
    private void extractDriverFiles() throws IOException {
    	
    	Path driverPath = Utils.getDriverDir();
    	driverPath.toFile().mkdirs();
    	
    	// get list of all drivers for this platform
    	String platformName = Platform.getCurrent().family().toString().toLowerCase();
    	String[] driverList = IOUtils.readLines(NodeStarter.class.getClassLoader().getResourceAsStream("driver-list.txt")).get(0).split(",");
   
    	for (String driverNameWithPf: driverList) {
    		if (!driverNameWithPf.startsWith(platformName)) {
    			continue;
    		}
    		String driverName = driverNameWithPf.replace(platformName + "/", "");
    		InputStream driver = NodeStarter.class.getClassLoader().getResourceAsStream(String.format("drivers/%s", driverNameWithPf));
    		try {
    			Files.copy(driver, Paths.get(driverPath.toString(), driverName), StandardCopyOption.REPLACE_EXISTING);
    			logger.info(String.format("Driver %s copied to %s", driverName, driverPath));
    		} catch (IOException e) {
    			logger.info(String.format("Driver not copied: %s - it may be in use", driverName));
    		}
        }
    	
    	// in case of Edge driver, copy the driver corresponding to windows version
    	if ("windows 10".equalsIgnoreCase(System.getProperty("os.name"))) {
    		String driverVersion = OSUtilityFactory.getInstance().getOSBuild().split("\\.")[2];
    		FileUtils.copyFile(Paths.get(driverPath.toString(), "MicrosoftWebDriver_" + driverVersion + ".exe").toFile(), 
    						   Paths.get(driverPath.toString(), "MicrosoftWebDriver.exe").toFile());
    	}
    	
    	// for IE copy the right version
    	if (OSUtilityFactory.getInstance().getIEVersion() < 10) {
    		FileUtils.copyFile(Paths.get(driverPath.toString(), "IEDriverServer_x64.exe").toFile(), 
					   			Paths.get(driverPath.toString(), "IEDriverServer.exe").toFile());
    	} else {
    		FileUtils.copyFile(Paths.get(driverPath.toString(), "IEDriverServer_Win32.exe").toFile(), 
		   						Paths.get(driverPath.toString(), "IEDriverServer.exe").toFile());
    	}
    }

    private void configure() throws IOException {
    	killExistingDrivers();
    	extractDriverFiles();
    	rewriteJsonConf();
    }

    private void start() throws Exception {
        GridLauncher.main(args);
    }
}
