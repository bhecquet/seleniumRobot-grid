package com.infotel.seleniumrobot.grid.tests.utils;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.BrowserManager;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.OSUtilityWindows;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.Platform;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class TestBrowserManager extends BaseMockitoTest {

    @Mock
    OSUtilityWindows osUtility;

    private MockedStatic<OSUtilityFactory> mockedOSUtilityFactory;
    private MockedStatic<OSUtility> mockedOSUtility;

    @BeforeMethod(groups = {"grid"})
    public void init() {

        mockedOSUtilityFactory = mockStatic(OSUtilityFactory.class);
        mockedOSUtility = mockStatic(OSUtility.class);

        mockedOSUtilityFactory.when(OSUtilityFactory::getInstance).thenReturn(osUtility);
        when(osUtility.getProgramExtension()).thenReturn("");
        mockedOSUtility.when(OSUtility::getCurrentPlatorm).thenReturn(Platform.LINUX);

    }

    @AfterMethod(groups = {"grid"}, alwaysRun = true)
    private void closeMocks() {
        mockedOSUtilityFactory.close();
        mockedOSUtility.close();
    }

    /**
     * Prepare profile
     * => profile deleted
     * => browser started and stopped
     * => default profile copied to "profiles/CHROME/RELEASE"
     */
    @Test(groups = {"grid"})
    public void testProfilePrepareChromeDefault() throws Exception {
        initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class)
        ) {
            ArgumentCaptor<String[]> options = ArgumentCaptor.forClass(String[].class);

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check chrome has been executed and killed
            verify(osUtility).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(options.capture()));
            Assert.assertEquals(options.getValue()[0], "/usr/bin/chrome");
            Assert.assertEquals(options.getValue()[1], "--no-first-run");
            Assert.assertTrue(options.getValue()[2].startsWith("--user-data-dir="));
        }
    }

    @Test(groups = {"grid"})
    public void testProfilePrepareEdgeDefault() throws Exception {
        initBrowserInfo(BrowserType.EDGE, "edge");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class)
        ) {
            ArgumentCaptor<String[]> options = ArgumentCaptor.forClass(String[].class);

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check chrome has been executed and killed
            verify(osUtility).killProcessByName("edge", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(options.capture()));
            Assert.assertEquals(options.getValue()[0], "/usr/bin/edge");
            Assert.assertEquals(options.getValue()[1], "--no-first-run");
            Assert.assertTrue(options.getValue()[2].startsWith("--user-data-dir="));
        }
    }

    @NotNull
    private String initBrowserInfo(BrowserType browserType, String browser) {
        Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
        BrowserInfo info = Mockito.spy(new BrowserInfo(browserType, "120.0", String.format("/usr/bin/%s", browser), false, false));
        String profilePath = info.getDefaultProfilePath();
        Mockito.doReturn(String.format("%sDriver", browser)).when(info).getDriverFileName();

        browsers.put(browserType, List.of(info));
        mockedOSUtility.when(OSUtility::getInstalledBrowsersWithVersion).thenReturn(browsers);
        return profilePath;
    }

}
