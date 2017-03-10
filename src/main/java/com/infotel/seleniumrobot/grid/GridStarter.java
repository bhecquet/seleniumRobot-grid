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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.selenium.GridLauncher;
import org.openqa.selenium.Platform;
import org.openqa.selenium.support.ui.SystemClock;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.config.NodeConfig;
import com.infotel.seleniumrobot.grid.config.capability.DesktopCapability;
import com.infotel.seleniumrobot.grid.config.capability.MobileCapability;
import com.infotel.seleniumrobot.grid.config.capability.NodeCapability;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.osutility.OSUtilityFactory;

public class GridStarter {
	
	private static final Logger logger = Logger.getLogger(GridStarter.class.getName());

	private LaunchConfig launchConfig;

    public GridStarter(String[] args) {
    	logger.info("starting grid v" + Utils.getCurrentversion());
        launchConfig = new LaunchConfig(args);
        
    }

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		((Appender)Logger.getRootLogger().getAllAppenders().nextElement()).setLayout(new PatternLayout("%-5p %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %C{1}: %m%n"));
		Logger.getRootLogger().setLevel(Level.INFO);
		
        GridStarter starter = new GridStarter(args);
        starter.configure();
        starter.start();
    }

    
    /**
     * Adds driver paths to configuration
     * @param nodeConf
     */
    private void addDriverToConfiguration(NodeConfig nodeConf) {
    	
    	String ext = OSUtilityFactory.getInstance().getProgramExtension();
		String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/");
		String platformName = Platform.getCurrent().family().toString().toLowerCase();
		
		nodeConf.getConfiguration().setChromeDriverPath(driverPath, ext);
		nodeConf.getConfiguration().setGeckoDriverPath(driverPath, ext);
		
		if ("windows".equals(platformName)) {
			nodeConf.getConfiguration().setIeDriverPath(driverPath, ext);
			nodeConf.getConfiguration().setEdgeDriverPath(driverPath, ext);
		}
    }
    
    private void addMobileDevicesToConfiguration(NodeConfig nodeConf) {
    	
    	List<NodeCapability> caps = nodeConf.getCapabilities();
    	
    	// handle android devices
    	try {
    		AdbWrapper adb = new AdbWrapper();
    		
    		for (MobileDevice device: adb.getDeviceList()) {
    			MobileCapability mobCap = new MobileCapability();
    			mobCap.setPlatformName("android");
    			mobCap.setPlatformVersion(device.getVersion());
    			mobCap.setDeviceName(device.getName());
    			mobCap.setBrowserName(StringUtils.join(device.getBrowsers(), ","));
    			caps.add(mobCap);
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
    private void addDesktopBrowsersToConfiguration(NodeConfig nodeConf) {
    	
    	List<NodeCapability> caps = nodeConf.getCapabilities();
    
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
    		
    		DesktopCapability cap = new DesktopCapability();
    		cap.setBrowserName(gridType);
    		cap.setPlatform(Platform.getCurrent().toString());
			caps.add(cap);
    	}
    }
    
    private void addBrowsersFromArguments(NodeConfig nodeConf) {
    	
    	List<NodeCapability> caps = nodeConf.getCapabilities();

		for (String browserConf: launchConfig.getBrowserConfig()) {
			DesktopCapability cap = new DesktopCapability();
			for (String pair: browserConf.split(",")) {
				String[] keyValue = pair.split("=", 2);
				if ("maxInstances".equals(keyValue[0])) {
					cap.setMaxInstances(Integer.parseInt(keyValue[1]));
				} else {
					cap.put(keyValue[0], keyValue[1]);
				}
			}
			caps.add(cap);
		}
    }
    
    /**
     * Method for generating json configuration in case none has been specified
     */
    public void rewriteJsonConf() {
    	if (!launchConfig.getHubRole() && launchConfig.getConfigPath() == null) {
    		try {
    			
				NodeConfig nodeConf = NodeConfig.buildDefaultConfig();
				addDriverToConfiguration(nodeConf);
				addMobileDevicesToConfiguration(nodeConf);
				addDesktopBrowsersToConfiguration(nodeConf);
				addBrowsersFromArguments(nodeConf);
				
				File newConfFile = Paths.get(Utils.getRootdir(), "generatedNodeConf.json").toFile();
				nodeConf.toJson(newConfFile);
				launchConfig.setConfigPath(newConfFile.getPath());
				
				// rewrite args with new configuration
				List<String> newArgs = new ArrayList<>();
				newArgs.addAll(Arrays.asList(launchConfig.getArgs()));
				newArgs.add(LaunchConfig.NODE_CONFIG);
				newArgs.add(newConfFile.getAbsolutePath());
				launchConfig.setArgs(newArgs.toArray(new String[0]));
				
			} catch (IOException e) {
				throw new GridException("Cannot generate conf file ", e);
			}
    	} 
    }
    
    private void killExistingDrivers() {
    	OSUtilityFactory.getInstance().killAllWebDriverProcess();
    }
    
    /**
     * in case of node, extract drivers
     * @throws IOException
     */
    private void extractDriverFiles() throws IOException {
    	
    	if (launchConfig.getHubRole()) {
    		return;
    	}
    	
    	Path driverPath = Utils.getDriverDir();
    	driverPath.toFile().mkdirs();
    	
    	// get list of all drivers for this platform
    	String platformName = Platform.getCurrent().family().toString().toLowerCase();
    	String[] driverList = IOUtils.readLines(GridStarter.class.getClassLoader().getResourceAsStream("driver-list.txt")).get(0).split(",");
   
    	for (String driverNameWithPf: driverList) {
    		if (!driverNameWithPf.startsWith(platformName)) {
    			continue;
    		}
    		String driverName = driverNameWithPf.replace(platformName + "/", "");
    		InputStream driver = GridStarter.class.getClassLoader().getResourceAsStream(String.format("drivers/%s", driverNameWithPf));
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
    
    /**
     * Check if node configuration is correct, else, exit
     * @throws IOException 
     */
    private void checkConfiguration() throws IOException {
    	
    	// check if we can get PID of this program
    	try {
    		Utils.getCurrentPID();
    	} catch (NumberFormatException e) {
    		logger.error("Cannot get current pid, use open JVM or Oracle JVM");
    		System.exit(1);
    	}
    	
    	// wait for port to be available
    	if (!launchConfig.getHubRole()) {
    		NodeConfig nodeConfig = NodeConfig.loadFromJson(new File(launchConfig.getConfigPath()));
    		waitForListenPortAvailability(nodeConfig.getConfiguration().getPort());
    	}
    }
    
    private void waitForListenPortAvailability(int port) {
    	SystemClock clock = new SystemClock();
    	long end = clock.laterBy(10000);
    	while (clock.isNowBefore(end)) {
    		if (!Utils.portAlreadyInUse(port)) {
    			return;
    		} else { 
    			logger.warn(String.format("Port %d already in use", port));
    			WaitHelper.waitForSeconds(1);
    		}
    	}
    	
    	// we are here if port is still in use
    	logger.error(String.format("could not start process as listen port %d is already in use", port));
    	System.exit(1);
    	
    }

    private void configure() throws IOException {
    	rewriteJsonConf();
    	checkConfiguration();
    	killExistingDrivers();
    	extractDriverFiles();
    }

    private void start() throws Exception {
        GridLauncher.main(launchConfig.getArgs());
    }

	public LaunchConfig getLaunchConfig() {
		return launchConfig;
	}
}
