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
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.cli.GridNodeCliOptions;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.grid.selenium.GridLauncherV3;
import org.openqa.grid.shared.Stoppable;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.server.SeleniumServer;
import org.openqa.selenium.remote.server.log.TerseFormatter;

import com.beust.jcommander.JCommander;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.server.GenericServlet;
import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.infotel.seleniumrobot.grid.utils.CommandLineOptionHelper;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.InstrumentsWrapper;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.logging.SeleniumRobotLogger;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;

import io.appium.java_client.remote.MobileCapabilityType;

public class GridStarter {
	
	private static Logger logger;
	private static final String[] NODE_SERVLETS = new String[] {"com.infotel.seleniumrobot.grid.servlets.server.MobileNodeServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.NodeStatusServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.FileServlet"};
	private static final String[] HUB_SERVLETS = new String[] {"com.infotel.seleniumrobot.grid.servlets.server.GuiServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.FileServlet",
													"com.infotel.seleniumrobot.grid.servlets.server.StatusServlet"};

	private LaunchConfig launchConfig;

    public GridStarter(String[] args) {
    	// for unit tests which do not use main
    	if (logger == null) {
    		logger = Logger.getLogger(GridStarter.class);
    	}
    	logger.info("starting grid v" + Utils.getCurrentversion());
        launchConfig = new LaunchConfig(args);
        
        if (!launchConfig.getDevMode()) {
        	logger.info("***************************************************************");
        	logger.info("DevMode=false: all browser sessions will be terminated on nodes");
        	logger.info("***************************************************************");
        }
        
    }

	public static void main(String[] args) throws Exception {
		args = initLoggers(args);
		writePidFile();
		
        GridStarter starter = new GridStarter(args);
        starter.configure();
        starter.start();
    }
	
	private static String[] initLoggers(String[] args) {
		
		String role = new LaunchConfig(args).getHubRole() ? "hub": "node";
		System.out.println(String.format("logs will be written to logs/%s-seleniumRobot-0.log", role));
		
		// init log4j logger
		BasicConfigurator.configure();
		Layout layout = new PatternLayout("%-5p %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %C{1}: %m%n");
		((Appender)Logger.getRootLogger().getAllAppenders().nextElement()).setLayout(layout);
		Logger.getRootLogger().setLevel(Level.INFO);
		logger = Logger.getLogger(GridStarter.class);
		SeleniumRobotLogger.updateLogger("logs", "logs", role + "-seleniumRobot-0.log", false);
		
		String[] newArgs = new String[] {"-log", String.format("logs/%s-seleniumRobot-%%g.log", role)};
		newArgs = ArrayUtils.addAll(newArgs, args);
		
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
    	
    	List<MutableCapabilities> caps = nodeConf.capabilities;
//    	String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/") + "/";
//		String ext = OSUtilityFactory.getInstance().getProgramExtension();
    	
    	// handle android devices
    	try {
    		AdbWrapper adb = new AdbWrapper();
    		
    		for (MobileDevice device: adb.getDeviceList()) {
    			MutableCapabilities deviceCaps = new MutableCapabilities();
    			deviceCaps.setCapability("maxInstances", 1);
    			deviceCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
    			deviceCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
    			deviceCaps.setCapability(MobileCapabilityType.PLATFORM_VERSION, device.getVersion());
    			deviceCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
    			deviceCaps.setCapability(MobileCapabilityType.DEVICE_NAME, device.getName());
    			deviceCaps.setCapability(MobileCapabilityType.BROWSER_NAME, StringUtils.join(device.getBrowsers()
    																						.stream()
    																						.map(BrowserInfo::getBrowser)
    																						.map(Object::toString)
    																						.map(String::toLowerCase)
    																						.collect(Collectors.toList()), ","));
//    			for (BrowserInfo bInfo: device.getBrowsers()) {
//    				switch(bInfo.getBrowser()) {
//		    			case BROWSER:
//		    				deviceCaps.setCapability(AppiumDriverProvider.ANDROID_DRIVER_EXE_PROPERTY, driverPath + bInfo.getDriverFileName() + ext);
//		    				break;
//		    			case CHROME:
//		    				deviceCaps.setCapability(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, driverPath + bInfo.getDriverFileName() + ext);
//		    				break;
//		    			default:
//    				}
//    			}
    			
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
    			deviceCaps.setCapability(MobileCapabilityType.PLATFORM_VERSION, device.getVersion());
    			deviceCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
    			deviceCaps.setCapability(MobileCapabilityType.DEVICE_NAME, device.getName());
    			deviceCaps.setCapability(MobileCapabilityType.BROWSER_NAME, StringUtils.join(device.getBrowsers(), ","));
    			caps.add(deviceCaps);
    		}
    		
    	} catch (ConfigurationException e) {
    		logger.info(e.getMessage());
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
    
    	for (Entry<BrowserType, List<BrowserInfo>> browserEntry: OSUtility.getInstalledBrowsersWithVersion().entrySet()) {
    		String gridType;
    		try {
    			Field browField = org.openqa.selenium.remote.BrowserType.class.getDeclaredField(browserEntry.getKey().name());
    			gridType = (String)browField.get(org.openqa.selenium.remote.BrowserType.class);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				if (browserEntry.getKey() == BrowserType.INTERNET_EXPLORER) {
					gridType = org.openqa.selenium.remote.BrowserType.IE;
				} else if (browserEntry.getKey() == BrowserType.BROWSER) {
					gridType = BrowserType.BROWSER.toString();
				} else {
					continue;
				}
			}
    		
    		for (BrowserInfo browserInfo: browserEntry.getValue()) {
	    		MutableCapabilities browserCaps = new MutableCapabilities();
	    		
	    		if (browserEntry.getKey() == BrowserType.INTERNET_EXPLORER) {
	    			browserCaps.setCapability("maxInstances", 1);
	    		} else {
	    			browserCaps.setCapability("maxInstances", 5);
	    		}
	    		browserCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
	    		browserCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
	    		browserCaps.setCapability("seleniumProtocol", "WebDriver");
	    		browserCaps.setCapability(CapabilityType.BROWSER_NAME, gridType);
	    		browserCaps.setCapability(CapabilityType.PLATFORM, Platform.getCurrent().toString());
	    		browserCaps.setCapability(CapabilityType.BROWSER_VERSION, browserInfo.getVersion());
	    		
	    		// add driver path
	    		if (browserInfo.getDriverFileName() != null) {
		    		switch(browserEntry.getKey()) {
		    			case FIREFOX:
		    				browserCaps.setCapability(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, driverPath + browserInfo.getDriverFileName() + ext);
		    				browserCaps.setCapability("firefox_binary", browserInfo.getPath());
		    				break;
		    			case CHROME:
		    				browserCaps.setCapability(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, driverPath + browserInfo.getDriverFileName() + ext);
		    				browserCaps.setCapability("chrome_binary", browserInfo.getPath());
		    				break;
		    			case INTERNET_EXPLORER:
		    				browserCaps.setCapability(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, driverPath + browserInfo.getDriverFileName() + ext);
		    				break;
		    			case EDGE:
		    				browserCaps.setCapability(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY, driverPath + browserInfo.getDriverFileName() + ext);
		    				break;
		    			default:
		    		}
	    		}
	    		caps.add(browserCaps);
    		}
    	}
    }
    
    private void addBrowsersFromArguments(GridNodeConfiguration nodeConf) {
    	
    	List<MutableCapabilities> caps = nodeConf.capabilities;

		for (String browserConf: launchConfig.getBrowserConfig()) {
			MutableCapabilities browserCap = new MutableCapabilities();
			for (String pair: browserConf.split(",")) {
				String[] keyValue = pair.split("=", 2);
				if ("maxInstances".equals(keyValue[0])) {
					browserCap.setCapability("maxInstances", Integer.parseInt(keyValue[1]));
				} else {
					browserCap.setCapability(keyValue[0], keyValue[1]);
				}
			}
			caps.add(browserCap);
		}
    }
    
    /**
     * Method for generating json configuration in case none has been specified
     */
    public void rewriteJsonConf() {
    	if (launchConfig.getConfigPath() == null) {
    		File newConfFile;
    		
	    	if (launchConfig.getHubRole()) {
	    		GridHubConfiguration hubConfiguration = new GridHubConfiguration();
	    		hubConfiguration.capabilityMatcher = new CustomCapabilityMatcher();
	    		hubConfiguration.registry = "com.infotel.seleniumrobot.grid.CustomGridRegistry";
	    		hubConfiguration.browserTimeout = 400; // https://github.com/SeleniumHQ/selenium/wiki/Grid2#configuring-timeouts-version-221-required
	    												// used to connect to grid node, to perform any operation related to browser
	    		hubConfiguration.timeout = 540; // when test crash or is stopped, avoid blocking session. Keep it above socket timeout of HttpClient (6 mins for mobile)
	    		hubConfiguration.newSessionWaitTimeout = 115000; // when new session is requested, send error before 2 minutes so that the source request from seleniumRobot does not go to timeout. It will then retry without letting staled new session requests
	    														 // (if this is set to -1: grid hub honours new session requests even if requester has closed request
//	    		hubConfiguration.servlets = Arrays.asList("com.infotel.seleniumrobot.grid.servlets.server.GuiServlet",
//	    													"com.infotel.seleniumrobot.grid.servlets.server.FileServlet");
	    		
	    		// workaround of issue https://github.com/SeleniumHQ/selenium/issues/6188
	    		List<String> argsWithServlet = new CommandLineOptionHelper(launchConfig.getArgList()).getAll();
	    		
	    		for (String servlet: HUB_SERVLETS) {
	    			argsWithServlet.add("-servlet");
	    			argsWithServlet.add(servlet);
	    		}
	    		launchConfig.setArgs(argsWithServlet);
	    		
	    		newConfFile = Paths.get(Utils.getRootdir(), "generatedHubConf.json").toFile();
				try {
					FileUtils.writeStringToFile(newConfFile, new Json().toJson(hubConfiguration.toJson()), Charset.forName("UTF-8"));
				} catch (IOException e) {
					throw new GridException("Cannot generate hub configuration file ", e);
				}	
	    		
	    	} else {
	    		try {
	    			GridNodeConfiguration nodeConf = new GridNodeConfiguration();
	    			nodeConf.capabilities = new ArrayList<>();
	    			
	    			nodeConf.proxy = "com.infotel.seleniumrobot.grid.CustomRemoteProxy";
	    			nodeConf.servlets = Arrays.asList(NODE_SERVLETS);
	    			nodeConf.nodeStatusCheckTimeout = 15; // wait only 15 secs

	    			nodeConf.timeout = 540; // when test crash or is stopped, avoid blocking session. Keep it above socket timeout of HttpClient (6 mins for mobile)
	    			
					addMobileDevicesToConfiguration(nodeConf);
					addDesktopBrowsersToConfiguration(nodeConf);
					addBrowsersFromArguments(nodeConf);
					
					newConfFile = Paths.get(Utils.getRootdir(), "generatedNodeConf.json").toFile();
					
					FileUtils.writeStringToFile(newConfFile, new Json().toJson(nodeConf.toJson()), Charset.forName("UTF-8"));
					launchConfig.setConfigPath(newConfFile.getPath());

				} catch (IOException e) {
					throw new GridException("Cannot generate node configuration file ", e);
				}
	    	}
	    	
	    	// rewrite args with new configuration
	    	List<String> newArgs = new CommandLineOptionHelper(launchConfig.getArgList()).removeAll("-browser");

			newArgs.add(launchConfig.getHubRole() ? LaunchConfig.HUB_CONFIG : LaunchConfig.NODE_CONFIG);
			newArgs.add(newConfFile.getAbsolutePath());
			launchConfig.setArgs(newArgs);
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
    	
    	ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();
        for(URL url: urls){ 
        	logger.info(url.getFile());
        }
    	
    	// get list of all drivers for this platform
    	String platformName = OSUtility.getCurrentPlatorm().toString().toLowerCase();
    	String[] driverList = IOUtils.readLines(GridStarter.class.getClassLoader().getResourceAsStream(String.format("driver-list-%s.txt", platformName)), Charset.forName("UTF-8")).get(0).split(",");
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
    		GridNodeConfiguration nodeConfig = GridNodeConfiguration.loadFromJSON(launchConfig.getConfigPath());
    		LaunchConfig.setCurrentNodeConfig(nodeConfig);
    		int jsonPort = nodeConfig.port;
    		int port = launchConfig.getNodePort() != null ? launchConfig.getNodePort() : jsonPort;
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
    }
    
    /**
     * Clean all directories where some temporary file could have been placed
     */
    private void cleanDirectories() {
    	try {
			FileUtils.deleteDirectory(Paths.get(Utils.getRootdir(), NodeTaskServlet.VIDEOS_FOLDER).toFile());
		} catch (IOException e) {
		}
    }
    
    /**
     * call all servlets to check if they are available
     */
    private void checkServletsAreUp(Stoppable stoppable) {
    	String[] servlets;
    	String servletRoot;
    	int port;
    	String host;
    	
    	if (launchConfig.getHubRole()) {
    		servlets = HUB_SERVLETS;
    		servletRoot = "/grid/admin/";
    		port = ((Hub)stoppable).getUrl().getPort();
    		host = ((Hub)stoppable).getUrl().getHost();
    		
    	} else {
    		servlets = NODE_SERVLETS;
    		servletRoot = "/extra/";
    		port = ((SeleniumServer)stoppable).getRealPort();
    		
    		GridNodeCliOptions options = new GridNodeCliOptions();
            JCommander commander = JCommander.newBuilder().addObject(options).build();
            commander.parse(launchConfig.getArgs());

            GridNodeConfiguration configuration = new GridNodeConfiguration(options);

    		host = "0.0.0.0".equals(configuration.host) ? "127.0.0.1": configuration.host;
    		LaunchConfig.setCurrentNodeConfig(configuration);
    	}

    	for (String servlet: servlets) {
			String name = servlet.substring(servlet.lastIndexOf(".") + 1, servlet.length());
			String url = String.format("http://%s:%d%s%s", host, port, servletRoot, name);
    		try {
				HttpResponse<String> response = Unirest.head(url + "/").asString();
				if (response.getStatus() != 200 && !response.getHeaders().containsKey(GenericServlet.SELENIUM_GRID_ALIVE_HEADER)) {
					throw new SeleniumGridException(String.format("cannot find servlet: %s at %s", servlet, url));
				}
			} catch (UnirestException e) {
				throw new SeleniumGridException(String.format("cannot contact servlet: %s at %s", servlet, url));
			}
    	}
    }

    private void start() throws Exception {
    	Stoppable server = new GridLauncherV3().launch(launchConfig.getArgs());
    	
    	String role = launchConfig.getHubRole() ? "hub": "node";
 
    	for (Handler handler : java.util.logging.Logger.getLogger("").getHandlers()) {
    		if (handler instanceof FileHandler) {
    			java.util.logging.Logger.getLogger("").removeHandler(handler);
    			handler.close();
    			
    			Handler logFile = new FileHandler(new File(String.format("logs/%s-seleniumRobot-%%g.log", role)).getAbsolutePath(), 20000000, 5, true);
    	        logFile.setFormatter(new TerseFormatter());
    	        logFile.setLevel(java.util.logging.Level.INFO);
    	        java.util.logging.Logger.getLogger("").addHandler(logFile);
    		}
    	}
    	
    	try {
    		checkServletsAreUp(server);
    	} catch (SeleniumGridException e) {
    		logger.error("Error while starting => stopping: " + e.getMessage());
    		System.exit(1);
    		server.stop();
    	}
        
    }

	public LaunchConfig getLaunchConfig() {
		return launchConfig;
	}
}
