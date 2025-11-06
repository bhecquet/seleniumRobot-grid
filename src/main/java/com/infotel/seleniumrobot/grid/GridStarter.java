/**
 * Copyright 2017 www.infotel.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infotel.seleniumrobot.grid;

import com.infotel.seleniumrobot.grid.aspects.SessionSlotActions;
import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.config.LaunchConfig.Role;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.mobile.LocalAppiumLauncher;
import com.infotel.seleniumrobot.grid.servlets.server.FileServlet;
import com.infotel.seleniumrobot.grid.servlets.server.WebServer;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.browserfactory.mobile.InstrumentsWrapper;
import com.seleniumtests.browserfactory.mobile.MobileDevice;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.logging.SeleniumRobotLogger;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.grid.Bootstrap;
import org.openqa.selenium.remote.CapabilityType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.seleniumtests.driver.BrowserType.EDGE;


public class GridStarter {

    public static final String APPIUM_PATH_ENV_VAR = "APPIUM_PATH";
    public static final String BROWSER_BINARY = "binary";
    private static Logger logger;
    private int browserStartupDelay = 15;

    private LaunchConfig launchConfig;

    public GridStarter(String[] args) {
        // for unit tests which do not use main
        if (logger == null) {
            logger = LogManager.getLogger(GridStarter.class);
        }
        logger.info("starting grid v{}", Utils.getCurrentversion());

        launchConfig = new LaunchConfig(args);


    }

    public static void main(String[] args) throws Exception {

        GridStarter starter = new GridStarter(args);

        if (starter.launchConfig.getRole() == Role.NODE && Boolean.FALSE.equals(starter.launchConfig.getDevMode())) {
            logger.info("***************************************************************");
            logger.info("DevMode=false: all browser sessions will be terminated on nodes");
            logger.info("***************************************************************");
        }

        initLoggers(starter.launchConfig.getRole());

        writePidFile();

        starter.configure();
        starter.start(starter.launchConfig.getArgs());
    }

    public GridStarter withBrowserStartupDelay(int browserStartupDelay) {
        this.browserStartupDelay = browserStartupDelay;
        return this;
    }

    private static void initLoggers(Role role) {
        logger = SeleniumRobotLogger.getLogger(GridStarter.class);

        SeleniumRobotLogger.updateLogger("logs", "logs", role + "-seleniumRobot-0.log", false);
        System.out.printf("logs will be written to logs/%s-seleniumRobot-0.log%n", role);
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


    /**
     * Add capabilities related to Windows tests to appium capabilities
     *
     * @param nodeConf      grid configuration
     * @param appiumDrivers list of appium drivers
     */
    private void addWindowsCapabilityToConfiguration(GridNodeConfiguration nodeConf, List<String> appiumDrivers) {

        List<MutableCapabilities> caps = nodeConf.appiumCapabilities;
        int existingCaps = caps.size();

        // handle windows
        try {
            if (OSUtility.isWindows() && appiumDrivers.contains("flaui")) {
                MutableCapabilities capabilities = new MutableCapabilities();
                capabilities.setCapability(CapabilityType.PLATFORM_NAME, "windows");
                capabilities.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
                capabilities.setCapability(LaunchConfig.MAX_SESSIONS, launchConfig.getMaxSessions());
                capabilities.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
                caps.add(capabilities);
            }
        } catch (ConfigurationException e) {
            logger.info(e.getMessage());
        }

        appiumWarning(caps, existingCaps);
    }

    private void addMobileDevicesToConfiguration(GridNodeConfiguration nodeConf, List<String> appiumDrivers) {

        List<MutableCapabilities> caps = nodeConf.appiumCapabilities;
        int existingCaps = caps.size();

        // handle android devices
        try {

            if (appiumDrivers.contains("uiautomator2")) {
                AdbWrapper adb = new AdbWrapper();
                for (MobileDevice device : adb.getDeviceList()) {

                    // mobile device for app testing
                    UiAutomator2Options deviceCaps = new UiAutomator2Options();
                    deviceCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
                    deviceCaps.setCapability(LaunchConfig.MAX_SESSIONS, launchConfig.getMaxSessions());
                    deviceCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
                    deviceCaps.setPlatformVersion(device.getVersion());
                    deviceCaps.setPlatformName("android");
                    deviceCaps.setDeviceName(device.getName());
                    deviceCaps.setCapability(CapabilityType.BROWSER_NAME, StringUtils.join(device.getBrowsers()
                            .stream()
                            .map(BrowserInfo::getBrowser)
                            .map(Object::toString)
                            .map(String::toLowerCase)
                            .toList(), ","));
                    caps.add(deviceCaps);

                }
            } else {
                logger.warn("UIAutomator2 appium driver is not installed");
            }

        } catch (ConfigurationException e) {
            logger.info(e.getMessage());
        }


        // handle ios devices
        try {
            InstrumentsWrapper instruments = new InstrumentsWrapper();
            if (appiumDrivers.contains("xcuitest")) {
                for (MobileDevice device : instruments.parseIosDevices()) {
                    XCUITestOptions deviceCaps = new XCUITestOptions();
                    deviceCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
                    deviceCaps.setCapability(LaunchConfig.MAX_SESSIONS, launchConfig.getMaxSessions());
                    deviceCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
                    deviceCaps.setPlatformVersion(device.getVersion());
                    deviceCaps.setPlatformName("iOS");
                    deviceCaps.setDeviceName(device.getName());
                    deviceCaps.setCapability(CapabilityType.BROWSER_NAME, StringUtils.join(device.getBrowsers(), ","));
                    caps.add(deviceCaps);
                }
            } else {
                logger.warn("XCUITest appium driver is not installed");
            }

        } catch (ConfigurationException e) {
            logger.info(e.getMessage());
        }


        appiumWarning(caps, existingCaps);
    }

    private static void appiumWarning(List<MutableCapabilities> caps, int existingCaps) {
        if (caps.size() - existingCaps > 0 && (System.getenv(APPIUM_PATH_ENV_VAR) == null || !new File(System.getenv(APPIUM_PATH_ENV_VAR)).exists())) {
            logger.error("********************************************************************************");
            logger.error("WARNING!!!");
            logger.error("Mobile nodes defined but APPIUM_PATH environment variable is not set or invalid");
            logger.error("********************************************************************************");
        }
    }

    /**
     * Add browser from user parameters
     */
    private void addDesktopBrowsersToConfiguration(GridNodeConfiguration nodeConf) {

        List<MutableCapabilities> caps = nodeConf.capabilities;
        String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/") + "/";
        String ext = OSUtilityFactory.getInstance().getProgramExtension();

        String edgePath = null;

        Map<BrowserType, List<BrowserInfo>> installedBrowsersWithVersion = OSUtility.getInstalledBrowsersWithVersion();

        // take non beta edge version for Edge in IE mode
        if (installedBrowsersWithVersion.get(EDGE) != null) {
            for (BrowserInfo browserInfo : installedBrowsersWithVersion.get(EDGE)) {
                if (!browserInfo.getBeta()) {
                    edgePath = browserInfo.getPath();
                }
            }
        }

        for (Entry<BrowserType, List<BrowserInfo>> browserEntry : installedBrowsersWithVersion.entrySet()) {

            String browserName = BrowserType.getSeleniumBrowserType(browserEntry.getKey());

            for (BrowserInfo browserInfo : browserEntry.getValue()) {
                MutableCapabilities browserCaps = new MutableCapabilities();

                // HTMLUnit is not supported on grid
                if (browserEntry.getKey() == BrowserType.HTMLUNIT) {
                    continue;
                }

                if (browserEntry.getKey() == BrowserType.INTERNET_EXPLORER) {
                    browserCaps.setCapability(LaunchConfig.TOTAL_SESSIONS, 1);
                    browserCaps.setCapability(LaunchConfig.MAX_SESSIONS, 1);
                } else {
                    browserCaps.setCapability(LaunchConfig.TOTAL_SESSIONS, launchConfig.getTotalSessions());
                    browserCaps.setCapability(LaunchConfig.MAX_SESSIONS, launchConfig.getMaxSessions());
                }
                browserCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, launchConfig.getNodeTags());
                browserCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, launchConfig.getRestrictToTags());
                browserCaps.setCapability(LaunchConfig.NODE_URL, launchConfig.getNodeUrl());
                browserCaps.setCapability(CapabilityType.BROWSER_NAME, browserName);
                browserCaps.setCapability(CapabilityType.PLATFORM_NAME, Platform.getCurrent().toString());
                browserCaps.setCapability(CapabilityType.BROWSER_VERSION, browserInfo.getVersion());
                browserCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, browserInfo.getBeta());

                addBrowserSpecificCapabilities(browserEntry, browserInfo, browserCaps, driverPath, ext, edgePath, caps);
            }
        }
    }

    private static void addBrowserSpecificCapabilities(Entry<BrowserType, List<BrowserInfo>> browserEntry, BrowserInfo browserInfo, MutableCapabilities browserCaps, String driverPath, String ext, String edgePath, List<MutableCapabilities> caps) {
        // add driver path
        try {
            if (browserInfo.getDriverFileName() != null) {
                switch (browserEntry.getKey()) {
                    case FIREFOX:
                        Map<String, Object> firefoxOptions = new HashMap<>();
                        firefoxOptions.put(BROWSER_BINARY, browserInfo.getPath().replace("\\", "/"));
                        browserCaps.setCapability(GridNodeConfiguration.WEBDRIVER_PATH, driverPath + browserInfo.getDriverFileName() + ext);
                        browserCaps.setCapability(FirefoxOptions.FIREFOX_OPTIONS, firefoxOptions);
                        browserCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, browserInfo.getDefaultProfilePath() == null ? "" : browserInfo.getDefaultProfilePath().replace("\\", "/"));
                        break;
                    case CHROME:
                        Map<String, Object> chromeOptions = new HashMap<>();
                        chromeOptions.put(BROWSER_BINARY, browserInfo.getPath().replace("\\", "/"));
                        browserCaps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
                        browserCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, browserInfo.getDefaultProfilePath() == null ? "" : browserInfo.getDefaultProfilePath().replace("\\", "/"));
                        break;
                    case INTERNET_EXPLORER:
                        browserCaps.setCapability(GridNodeConfiguration.WEBDRIVER_PATH, driverPath + browserInfo.getDriverFileName() + ext);
                        browserCaps.setCapability(SessionSlotActions.EDGE_PATH, edgePath == null ? "" : edgePath.replace("\\", "/"));
                        break;
                    case EDGE:
                        Map<String, Object> edgeOptions = new HashMap<>();
                        edgeOptions.put(BROWSER_BINARY, browserInfo.getPath().replace("\\", "/"));
                        browserCaps.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
                        browserCaps.setCapability(LaunchConfig.DEFAULT_PROFILE_PATH, browserInfo.getDefaultProfilePath() == null ? "" : browserInfo.getDefaultProfilePath().replace("\\", "/"));
                        break;
                    default:
                }
            }
            caps.add(browserCaps);
        } catch (ConfigurationException e) {
            logger.warn("Browser {} will be disabled: {}", browserInfo.getBrowser(), e.getMessage());
        }
    }

    /**
     * Chrome & Edge share the same process, so only do minimal check with Edge
     */
    private void cleanBrowserProfile(BrowserInfo browserInfo) {

        // in case folder does not exist, create it
        long profileSize = 10000000000L;
        try {
            if (browserInfo.getDefaultProfilePath() != null) {
                profileSize = FileUtils.sizeOfDirectory(new File(browserInfo.getDefaultProfilePath()));
            }
        } catch (UncheckedIOException e) {
            // ignore
        }

        if (Boolean.TRUE.equals(launchConfig.doCleanBrowserProfile())
                && browserInfo.getDefaultProfilePath() != null
                && profileSize > 150000000L) {
            String processName = new File(browserInfo.getPath()).getName().split("\\.")[0];
            logger.info("Cleaning {} user data", browserInfo.getBrowser());
            try {

                // check if browser is started. If yes, close it if not in devmode so that
                // this is necessary so that files can be deleted
                if (Boolean.FALSE.equals(launchConfig.getDevMode())) {
                    List<ProcessInfo> browserProcesses = OSUtilityFactory.getInstance().getRunningProcesses(processName);

                    logger.info("Killing {} to allow cleaning user data", processName);
                    if (!browserProcesses.isEmpty()) {
                        OSUtilityFactory.getInstance().killProcessByName(processName, true);
                        WaitHelper.waitForSeconds(3);
                    }
                }

                List<ProcessInfo> browserProcesses = OSUtilityFactory.getInstance().getRunningProcesses(processName);
                if (browserProcesses.isEmpty()) {
                    if (Paths.get(browserInfo.getDefaultProfilePath()).toFile().exists()) {
                        FileUtils.deleteDirectory(Paths.get(browserInfo.getDefaultProfilePath()).toFile());
                    }
                    OSCommand.executeCommand(new String[]{browserInfo.getPath()});
                    logger.info("Wait {} seconds that extensions managed by enterprise get installed", browserStartupDelay);
                    WaitHelper.waitForSeconds(browserStartupDelay); // wait browser start
                    OSUtilityFactory.getInstance().killProcessByName(processName, true);
                    WaitHelper.waitForSeconds(3); // wait for process to be stopped so that lockfile get removed
                }
            } catch (IOException e) {
                logger.warn("could not delete profile folder: {}", e.getMessage());
            }
        }
    }

    /**
     * Method for generating json configuration
     */
    public void rewriteJsonConf() {
        File newConfFile;

        if (launchConfig.getRole() == Role.NODE) {
            try {

                GridNodeConfiguration nodeConf = new GridNodeConfiguration();
                nodeConf.capabilities = new ArrayList<>();
                LocalAppiumLauncher appiumLauncher = new LocalAppiumLauncher("logs");

                List<String> appiumDrivers = new ArrayList<>();
                try {
                    appiumDrivers = appiumLauncher.getDriverList();
                } catch (ConfigurationException e) {
                    logger.warn(e.getMessage());
                    logger.warn("Appium not installed skipping driver list");
                }

                addMobileDevicesToConfiguration(nodeConf, appiumDrivers);
                addDesktopBrowsersToConfiguration(nodeConf);
                addWindowsCapabilityToConfiguration(nodeConf, appiumDrivers);

                if (!nodeConf.appiumCapabilities.isEmpty()) {
                    OSUtilityFactory.getInstance().killProcessByName("appium", true);
                    OSUtilityFactory.getInstance().killProcessByName("node", true);
                    WaitHelper.waitForSeconds(1); // be sure we do not kill node while appium is starting
                    logger.info("Starting appium");

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
     *
     * @throws IOException
     */
    private void extractDriverFiles() throws IOException {

        if (launchConfig.getRole() == Role.NODE) {
            Path driverPath = Utils.getDriverDir();
            driverPath.toFile().mkdirs();

            // get list of all drivers for this platform
            String platformName = OSUtility.getCurrentPlatorm().toString().toLowerCase();
            String[] driverList = IOUtils.readLines(GridStarter.class.getClassLoader().getResourceAsStream(String.format("driver-list-%s.txt", platformName)), StandardCharsets.UTF_8).get(0).split(",");
            List<String> platformDriverNames = new ArrayList<>();

            for (String driverNameWithPf : driverList) {
                if (!driverNameWithPf.replace("unix", "linux").startsWith(platformName)) {
                    continue;
                }
                String driverName = driverNameWithPf.replace("unix", "linux").replace(platformName + "/", "");
                platformDriverNames.add(driverName);
                InputStream driver = GridStarter.class.getClassLoader().getResourceAsStream(String.format("drivers/%s", driverNameWithPf));
                if (driver != null) {
                    try {
                        Path driverFilePath = Paths.get(driverPath.toString(), driverName);
                        Files.copy(driver, driverFilePath, StandardCopyOption.REPLACE_EXISTING);
                        driverFilePath.toFile().setExecutable(true, false);
                        logger.info("Driver {} copied to {}", driverName, driverPath);
                    } catch (IOException e) {
                        logger.info("Driver not copied: {} - it may be in use", driverName);
                    }
                }
            }

            // send driver names to BrowserInfo so that they can be used for version matching
            BrowserInfo.setDriverList(platformDriverNames);
        }
    }

    /**
     * Check if node configuration is correct, else, exit
     */
    private void checkConfiguration() {

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
                logger.warn("Port {} already in use", port);
                WaitHelper.waitForSeconds(1);
            }
        }

        // we are here if port is still in use
        logger.error("could not start process as listen port {} is already in use", port);
        System.exit(1);

    }

    public void initializeProfiles() throws IOException {
        if (launchConfig.getRole() == Role.NODE) {
            FileUtils.deleteDirectory(Utils.getProfilesDir().toFile());
            Files.createDirectories(Utils.getProfilesDir());

            Map<BrowserType, List<BrowserInfo>> installedBrowsersWithVersion = OSUtility.getInstalledBrowsersWithVersion();

            installedBrowsersWithVersion.entrySet().stream()
                    .flatMap(browserInfoEntry -> browserInfoEntry.getValue().stream()
                            .filter(browserInfo -> browserInfo.getDriverFileName() != null)
                            .map(browserInfo -> Map.entry(browserInfoEntry.getKey(), browserInfo)))
                    .forEach(entry -> {
                        BrowserType type = entry.getKey();
                        BrowserInfo info = entry.getValue();
                        if (type == BrowserType.EDGE || type == BrowserType.CHROME) {
                            cleanBrowserProfile(info);

                            // copy default profile to a new folder that will be used
                            Path defaultProfilePath = copyDefaultProfile(info);
                            info.setDefaultProfilePath(defaultProfilePath.toFile().getAbsolutePath());
                        }
                    });

        }
    }

    private Path copyDefaultProfile(BrowserInfo browserInfo) {
        Path tempProfile;
        try {
            tempProfile = Files.createDirectories(Utils.getProfilesDir().resolve(browserInfo.getBrowser().name()).resolve(browserInfo.getBeta() ? "Beta" : "Release"));
            FileUtils.copyDirectory(new File(browserInfo.getDefaultProfilePath()), tempProfile.toFile());
        } catch (IOException e) {
            throw new SeleniumGridException("Cannot create profile directory", e);
        }

        return tempProfile;
    }

    public void configure() throws IOException {
        initializeProfiles();
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
            logger.info("Adding router servlets on port {}", (LaunchConfig.getCurrentLaunchConfig().getRouterPort() + 10));
        }
    }

    /**
     * Clean all directories where some temporary file could have been placed
     */
    private void cleanDirectories() {
        try {
            FileUtils.deleteDirectory(Paths.get(Utils.getRootdir(), GridNodeConfiguration.VIDEOS_FOLDER).toFile());
        } catch (IOException e) {
            //
        }
        try {
            FileUtils.deleteDirectory(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR).toFile());
        } catch (IOException e) {
            //
        }
        // create upload directory
        try {
            Files.createDirectories(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR));
        } catch (IOException e) {
            logger.error("Could not create upload directory");
        }
    }

    public void start(String[] args) {
        Bootstrap.main(args);
    }

    public LaunchConfig getLaunchConfig() {
        return launchConfig;
    }

}
