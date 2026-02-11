package com.infotel.seleniumrobot.grid.utils;

import com.infotel.seleniumrobot.grid.GridStarter;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrowserManager {

    private static final Logger logger = LogManager.getLogger(BrowserManager.class);
    private LaunchConfig launchConfig;
    private int browserStartupDelay = 15;

    public BrowserManager(LaunchConfig launchConfig) {
        this.launchConfig = launchConfig;
    }

    /**
     * Chrome & Edge share the same process, so only do minimal check with Edge
     */
    private void cleanBrowserProfile(BrowserInfo browserInfo) {

        // in case folder does not exist, create it
        long profileSize = 100L;
        try {
            if (browserInfo.getDefaultProfilePath() != null) {
                profileSize = FileUtils.sizeOfDirectory(new File(browserInfo.getDefaultProfilePath()));
            }
        } catch (UncheckedIOException e) {
            // ignore
        }

        if ((Boolean.TRUE.equals(launchConfig.doCleanBrowserProfile())
                && browserInfo.getDefaultProfilePath() != null
                && profileSize > 150000000L)
                || profileSize < 10000000L // in case profile has not been created before, create it by starting browser
        ) {
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


    private Path copyDefaultChromiumProfile(BrowserInfo browserInfo) {
        Path tempProfile;
        try {
            tempProfile = Files.createDirectories(Utils.getProfilesDir().resolve(browserInfo.getBrowser().name()).resolve(browserInfo.getBeta() ? "Beta" : "Release"));
            FileUtils.copyDirectory(new File(browserInfo.getDefaultProfilePath()), tempProfile.toFile());

        } catch (IOException e) {
            throw new SeleniumGridException("Cannot create profile directory", e);
        }

        return tempProfile;
    }

    /**
     * Copy the missing chromium extensions
     *
     * @param browserInfo browserinfo for this browser
     */
    public void restoreChromiumExtensions(BrowserInfo browserInfo) {

        try {
            Path tempExtensionPath = Utils.getProfilesDir()
                    .resolve(browserInfo.getBrowser().name())
                    .resolve(browserInfo.getBeta() ? "Beta" : "Release")
                    .resolve("Default")
                    .resolve("Extensions");

            Path defaultExtensionPath = Paths.get(browserInfo.getDefaultProfilePath())
                    .resolve("Default")
                    .resolve("Extensions");

            for (File extensionFolder : FileUtils.listFiles(defaultExtensionPath.toFile(), TrueFileFilter.INSTANCE, null)) {
                if (!tempExtensionPath.resolve(extensionFolder.getName()).toFile().exists()) {
                    FileUtils.copyDirectory(defaultExtensionPath.resolve(extensionFolder.getName()).toFile(),
                            tempExtensionPath.resolve(extensionFolder.getName()).toFile());
                }
            }

        } catch (IOException e) {
            logger.error("Cannot update extensions", e);
        }
    }

    public void killExistingDrivers() {
        OSUtilityFactory.getInstance().killAllWebDriverProcess();
    }

    public void extractDriverFiles() {

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

    public void initializeProfiles() throws IOException {
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
                        Path defaultProfilePath = copyDefaultChromiumProfile(info);
                        info.setDefaultProfilePath(defaultProfilePath.toFile().getAbsolutePath());
                    }
                });
    }


    public BrowserManager withBrowserStartupDelay(int browserStartupDelay) {
        this.browserStartupDelay = browserStartupDelay;
        return this;
    }
}
