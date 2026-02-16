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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    private int browserStartupDelay = 10;

    public BrowserManager(LaunchConfig launchConfig) {
        this.launchConfig = launchConfig;
    }

    /**
     * Chrome & Edge share the same process, so only do minimal check with Edge
     */
    private Path prepareChromiumBrowserProfile(BrowserInfo browserInfo) {

        try {
            Path tempProfile = Files.createDirectories(Utils.getProfilesDir().resolve(browserInfo.getBrowser().name()).resolve(browserInfo.getBeta() ? "Beta" : "Release"));

            String processName = new File(browserInfo.getPath()).getName().split("\\.")[0];
            logger.info("Preparing {} user data in {}", browserInfo.getBrowser(), tempProfile);

            OSCommand.executeCommand(new String[]{browserInfo.getPath(), "--no-first-run", "--user-data-dir=" + tempProfile});
            logger.info("Wait {} seconds that extensions managed by enterprise get installed", browserStartupDelay);
            WaitHelper.waitForSeconds(browserStartupDelay); // wait browser start
            OSUtilityFactory.getInstance().killProcessByName(processName, true);

            return tempProfile;

        } catch (IOException e) {
            throw new SeleniumGridException("Cannot create profile directory", e);
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
        try {
            FileUtils.deleteDirectory(Utils.getProfilesDir().toFile());
        } catch (IOException e) {
            logger.warn("Cannot delete profile directory: {}", Utils.getProfilesDir());
        }
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
                        Path defaultProfilePath = prepareChromiumBrowserProfile(info);
                        info.setDefaultProfilePath(defaultProfilePath.toFile().getAbsolutePath());
                    }
                });
    }


    public BrowserManager withBrowserStartupDelay(int browserStartupDelay) {
        this.browserStartupDelay = browserStartupDelay;
        return this;
    }
}
