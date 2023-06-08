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
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.grid.Bootstrap;
import org.openqa.selenium.remote.CapabilityType;

import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.config.LaunchConfig.Role;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.server.WebServer;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.InstrumentsWrapper;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.logging.SeleniumRobotLogger;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;

import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;

public class GridStarter {
	
	private static Logger logger;
	private static final String[] NODE_SERVLETS = new String[] {"com.infotel.seleniumrobot.grid.servlets.server.MobileNodeServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.NodeStatusServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.FileServlet"};
	private static final String[] HUB_SERVLETS = new String[] {"com.infotel.seleniumrobot.grid.servlets.server.GuiServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.FileServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.StatusServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.HubTaskServlet",
													};

	private LaunchConfig launchConfig;

    public GridStarter(String[] args) {
    	// for unit tests which do not use main
    	if (logger == null) {
    		logger = LogManager.getLogger(GridStarter.class);
    	}
    	logger.info("starting grid v" + Utils.getCurrentversion());

        launchConfig = new LaunchConfig(args);

        
    }

	public static void main(String[] args) throws Exception {
		
        GridStarter starter = new GridStarter(args);

		if (starter.launchConfig.getRole() == Role.NODE &&  !starter.launchConfig.getDevMode()) {
			logger.info("***************************************************************");
			logger.info("DevMode=false: all browser sessions will be terminated on nodes");
			logger.info("***************************************************************");
		}

		args = initLoggers(starter.launchConfig.getRole(), starter.launchConfig.getArgs());
		
		writePidFile();
        
        starter.configure();
        starter.start(starter.launchConfig.getArgs());
    }
	
	private static String[] initLoggers(Role role, String[] args) {
		logger = SeleniumRobotLogger.getLogger(GridStarter.class);
		
		SeleniumRobotLogger.updateLogger("logs", "logs", role + "-seleniumRobot-0.log", false); 
		System.out.println(String.format("logs will be written to logs/%s-seleniumRobot-0.log", role));

		String[] newArgs = new String[] {"--log", String.format("logs/%s-seleniumRobot-0.log", role)};
		newArgs = ArrayUtils.addAll(args, newArgs);
		
		return newArgs;
	}
	
	private static void writePidFile() {
		try {
			final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
	        final int index = jvmName.indexOf('@');
	        FileUtils.write(new File(Utils.getRootdir() + "/pid"), String.valueOf(Long.parseLong(jvmName.substring(0, index))), Charset.defaultCharset());
		} catch (Exception e) {
			logger.warn("cannot write PID file");
		}
	}

    private void addMobileDevicesToConfiguration(GridNodeConfiguration nodeConf) {
    	
    	List<MutableCapabilities> caps = nodeConf.mobileCapabilities;
    	int existingCaps = caps.size();
    	
    	
    	// handle android devices
    	try {
    		AdbWrapper adb = new AdbWrapper();
    		
    		for (MobileDevice device: adb.getDeviceList()) {
    			MutableCapabilities deviceCaps = new MutableCapabilities();
    			deviceCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
    			deviceCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
    			deviceCaps.setCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + MobileCapabilityType.PLATFORM_VERSION, device.getVersion());
    			deviceCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
    			deviceCaps.setCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + MobileCapabilityType.DEVICE_NAME, device.getName());
//    			deviceCaps.setCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + MobileCapabilityType.UDID, device.getId());
    			deviceCaps.setCapability(MobileCapabilityType.BROWSER_NAME, StringUtils.join(device.getBrowsers()
    																						.stream()
    																						.map(BrowserInfo::getBrowser)
    																						.map(Object::toString)
    																						.map(String::toLowerCase)
    																						.collect(Collectors.toList()), ","));

    			caps.add(deviceCaps);
    		}
    		
    	} catch (ConfigurationException e) {
    		logger.info(e.getMessage());
    	}
    	
    	// handle ios devices
    	try {
    		InstrumentsWrapper instruments = new InstrumentsWrapper();		
    		for (MobileDevice device: instruments.parseIosDevices()) {			
    			MutableCapabilities deviceCaps = new MutableCapabilities();
    			deviceCaps.setCapability("maxInstances", 1);
    			deviceCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
    			deviceCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
    			deviceCaps.setCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + MobileCapabilityType.PLATFORM_VERSION, device.getVersion());
    			deviceCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
    			deviceCaps.setCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + MobileCapabilityType.DEVICE_NAME, device.getName());
    			deviceCaps.setCapability(MobileCapabilityType.BROWSER_NAME, StringUtils.join(device.getBrowsers(), ","));
    			caps.add(deviceCaps);
    		}
    		
    	} catch (ConfigurationException e) {
    		logger.info(e.getMessage());
    	}
    	
    	if (caps.size() - existingCaps > 0 && (System.getenv("APPIUM_HOME") == null || !new File(System.getenv("APPIUM_HOME")).exists())) {
    		logger.error("********************************************************************************");
    		logger.error("WARNING!!!");
    		logger.error("Mobile nodes defined but APPIUM_HOME environment variable is not set or invalid");
    		logger.error("********************************************************************************");
    	}
    }
    
    /**
     * Add browser from user parameters
     * @param nodeConf
     */
    private void addDesktopBrowsersToConfiguration(GridNodeConfiguration nodeConf) {
    	
    	List<MutableCapabilities> caps = nodeConf.capabilities;
    	String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/") + "/";
		String ext = OSUtilityFactory.getInstance().getProgramExtension();
		
		String edgePath = null;
    
    	Map<BrowserType, List<BrowserInfo>> installedBrowsersWithVersion = OSUtility.getInstalledBrowsersWithVersion();
    	
    	// take non beta edge version for Edge in IE mode
    	if (installedBrowsersWithVersion.get(BrowserType.EDGE) != null) {
    		for (BrowserInfo browserInfo: installedBrowsersWithVersion.get(BrowserType.EDGE)) {
    			if (!browserInfo.getBeta()) {
    				edgePath = browserInfo.getPath();
    			}
    		}
    	}

		for (Entry<BrowserType, List<BrowserInfo>> browserEntry: installedBrowsersWithVersion.entrySet()) {

    		String browserName = BrowserType.getSeleniumBrowserType(browserEntry.getKey());

    		for (BrowserInfo browserInfo: browserEntry.getValue()) {
	    		MutableCapabilities browserCaps = new MutableCapabilities();
	    		
	    		// HTMLUnit is not supported on grid
	    		if (browserEntry.getKey() == BrowserType.HTMLUNIT) {
	    			continue;
	    		}
	    		
	    		if (browserEntry.getKey() == BrowserType.INTERNET_EXPLORER) {
	    			browserCaps.setCapability("max-sessions", 1);
	    		} else {
	    			browserCaps.setCapability("max-sessions", 5);
	    		}
	    		browserCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
	    		browserCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
	    		browserCaps.setCapability(CapabilityType.BROWSER_NAME, browserName);
	    		browserCaps.setCapability(CapabilityType.PLATFORM_NAME, Platform.getCurrent().toString());
	    		browserCaps.setCapability(CapabilityType.BROWSER_VERSION, browserInfo.getVersion());
	    		browserCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, browserInfo.getBeta());
	    		
	    		// add driver path
	    		try {
		    		if (browserInfo.getDriverFileName() != null) {
			    		switch(browserEntry.getKey()) {
			    			case FIREFOX:
			    				browserCaps.setCapability(GridNodeConfiguration.WEBDRIVER_PATH, driverPath + browserInfo.getDriverFileName() + ext);
			    				browserCaps.setCapability("firefox_binary", browserInfo.getPath().replace("\\", "/"));
			    				browserCaps.setCapability("defaultProfilePath", browserInfo.getDefaultProfilePath() == null ? "": browserInfo.getDefaultProfilePath().replace("\\", "/"));
			    				break;
			    			case CHROME:
			    				browserCaps.setCapability(GridNodeConfiguration.WEBDRIVER_PATH, driverPath + browserInfo.getDriverFileName() + ext);
			    				browserCaps.setCapability("chrome_binary", browserInfo.getPath().replace("\\", "/"));
			    				browserCaps.setCapability("defaultProfilePath", browserInfo.getDefaultProfilePath() == null ? "": browserInfo.getDefaultProfilePath().replace("\\", "/"));
			    				break;
			    			case INTERNET_EXPLORER:
			    				browserCaps.setCapability(GridNodeConfiguration.WEBDRIVER_PATH, driverPath + browserInfo.getDriverFileName() + ext);
			    				browserCaps.setCapability("edgePath", edgePath == null ? "": edgePath.replace("\\", "/"));
			    				break;
			    			case EDGE:
			    				browserCaps.setCapability(GridNodeConfiguration.WEBDRIVER_PATH, driverPath + browserInfo.getDriverFileName() + ext);
			    				browserCaps.setCapability("edge_binary", browserInfo.getPath().replace("\\", "/"));
			    				break;
			    			default:
			    		}
		    		}
		    		caps.add(browserCaps);
	    		} catch (ConfigurationException e) {
	    			logger.warn(String.format("Browser %s will be disabled: %s", browserInfo.getBrowser(), e.getMessage()));
	    		}
    		}
    	}
    }
    
    /**
     * Method for generating json configuration 
     */
    public void rewriteJsonConf() {
		File newConfFile;
		
    	if (launchConfig.getRole() == Role.HUB) {
//	    		GridHubConfiguration hubConfiguration = new GridHubConfiguration();
//	    		hubConfiguration.capabilityMatcher = new CustomCapabilityMatcher();
//	    		hubConfiguration.registry = "com.infotel.seleniumrobot.grid.CustomGridRegistry";
//	    		hubConfiguration.browserTimeout = 400; // https://github.com/SeleniumHQ/selenium/wiki/Grid2#configuring-timeouts-version-221-required
//	    												// used to connect to grid node, to perform any operation related to browser
//	    		hubConfiguration.timeout = 540; // when test crash or is stopped, avoid blocking session. Keep it above socket timeout of HttpClient (6 mins for mobile)
//	    		hubConfiguration.newSessionWaitTimeout = 115000; // when new session is requested, send error before 2 minutes so that the source request from seleniumRobot does not go to timeout. It will then retry without letting staled new session requests
//	    														 // (if this is set to -1: grid hub honours new session requests even if requester has closed request
//
//	    		// workaround of issue https://github.com/SeleniumHQ/selenium/issues/6188
//	    		List<String> argsWithServlet = new CommandLineOptionHelper(launchConfig.getArgList()).getAll();
//
//	    		for (String servlet: HUB_SERVLETS) {
//	    			argsWithServlet.add("-servlet");
//	    			argsWithServlet.add(servlet);
//	    		}
//	    		launchConfig.setArgs(argsWithServlet);
//
//	    		newConfFile = Paths.get(Utils.getRootdir(), "generatedHubConf.json").toFile();
//				try {
//					FileUtils.writeStringToFile(newConfFile, new Json().toJson(hubConfiguration.toJson()), StandardCharsets.UTF_8);
//				} catch (IOException e) {
//					throw new GridException("Cannot generate hub configuration file ", e);
//				}



    	} else if (launchConfig.getRole() == Role.NODE) {
    		try {
    			
    			GridNodeConfiguration nodeConf = new GridNodeConfiguration();
    			nodeConf.capabilities = new ArrayList<>();
    			
//    			nodeConf.proxy = "com.infotel.seleniumrobot.grid.CustomRemoteProxy";
//    			nodeConf.servlets = Arrays.asList(NODE_SERVLETS);
//    			nodeConf.nodeStatusCheckTimeout = 15; // wait only 15 secs
//    			nodeConf.enablePlatformVerification = false;

				addMobileDevicesToConfiguration(nodeConf);
				addDesktopBrowsersToConfiguration(nodeConf);
				
				if (!nodeConf.mobileCapabilities.isEmpty()) {
					OSUtilityFactory.getInstance().killProcessByName("appium", true);
					OSUtilityFactory.getInstance().killProcessByName("node", true);
					logger.info("Starting appium");
					LocalAppiumLauncher appiumLauncher = new LocalAppiumLauncher("logs");
					if (Integer.parseInt(appiumLauncher.getAppiumVersion().split("\\.")[0]) >= 2) {
						nodeConf.appiumUrl = String.format("http://localhost:%d", appiumLauncher.getAppiumPort());
					} else {
						nodeConf.appiumUrl = String.format("http://localhost:%d/wd/hub", appiumLauncher.getAppiumPort());
					}
					appiumLauncher.startAppiumWithWait();
				}
					
				
				newConfFile = Paths.get(Utils.getRootdir(), "generatedNodeConf.toml").toFile();
				FileUtils.writeStringToFile(newConfFile, nodeConf.toToml(), StandardCharsets.UTF_8);
				launchConfig.setConfigPath(newConfFile.getPath());
				LaunchConfig.setCurrentNodeConfig(nodeConf);

			} catch (IOException e) {
				throw new SeleniumGridException("Cannot generate node configuration file ", e);
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
    	
    	if (launchConfig.getRole() == Role.NODE) {
    		Path driverPath = Utils.getDriverDir();
        	driverPath.toFile().mkdirs();
        	
        	ClassLoader cl = ClassLoader.getSystemClassLoader();
            URL[] urls = ((URLClassLoader)cl).getURLs();
            for(URL url: urls){ 
            	logger.info(url.getFile());
            }
        	
        	// get list of all drivers for this platform
        	String platformName = OSUtility.getCurrentPlatorm().toString().toLowerCase();
        	String[] driverList = IOUtils.readLines(GridStarter.class.getClassLoader().getResourceAsStream(String.format("driver-list-%s.txt", platformName)), StandardCharsets.UTF_8).get(0).split(",");
        	List<String> platformDriverNames = new ArrayList<>();
        	
        	for (String driverNameWithPf: driverList) {
        		if (!driverNameWithPf.replace("unix", "linux").startsWith(platformName)) {
        			continue;
        		}
        		String driverName = driverNameWithPf.replace("unix", "linux").replace(platformName + "/", "");
        		platformDriverNames.add(driverName);
        		InputStream driver = GridStarter.class.getClassLoader().getResourceAsStream(String.format("drivers/%s", driverNameWithPf));
        		try {
        			Path driverFilePath = Paths.get(driverPath.toString(), driverName);
        			Files.copy(driver, driverFilePath, StandardCopyOption.REPLACE_EXISTING);
        			driverFilePath.toFile().setExecutable(true, false);
        			logger.info(String.format("Driver %s copied to %s", driverName, driverPath));
        		} catch (IOException e) {
        			logger.info(String.format("Driver not copied: %s - it may be in use", driverName));
        		}
            }
        	
        	// send driver names to BrowserInfo so that they can be used for version matching
        	BrowserInfo.setDriverList(platformDriverNames);
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
    	if (launchConfig.getRole() == Role.NODE) {

    		int port = launchConfig.getNodePort() != null ? launchConfig.getNodePort() : 0;
    		waitForListenPortAvailability(port);

    	}
    }
    
    private void waitForListenPortAvailability(int port) {
    	Clock clock = Clock.systemUTC();
    	Instant end = clock.instant().plusSeconds(15);
    	while (end.isAfter(clock.instant())) {
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
    	extractDriverFiles();
    	rewriteJsonConf();
    	checkConfiguration();
    	killExistingDrivers();
    	cleanDirectories();
    	startServlets();
    }

    private void startServlets() {
    	if (LaunchConfig.getCurrentLaunchConfig().getRole() == Role.HUB || (LaunchConfig.getCurrentLaunchConfig().getRole() == Role.ROUTER)) {
			try {
				new WebServer().startRouterServletServer(LaunchConfig.getCurrentLaunchConfig().getRouterPort() + 10);
			} catch (Exception e) {
				throw new SeleniumGridException("Error starting servlet server");
			}
			logger.info("Adding router servlets on port " + (LaunchConfig.getCurrentLaunchConfig().getRouterPort() + 10));
		}
	}
    
    /**
     * Clean all directories where some temporary file could have been placed
     */
    private void cleanDirectories() {
    	try {
			FileUtils.deleteDirectory(Paths.get(Utils.getRootdir(), GridNodeConfiguration.VIDEOS_FOLDER).toFile());
		} catch (IOException e) {
		}
    }

    private void start(String[] args) throws Exception {	
    	Bootstrap.main(args);
    }

	public LaunchConfig getLaunchConfig() {
		return launchConfig;
	}

}
