package com.infotel.seleniumrobot.grid.tests.utils;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.BrowserManager;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.osutility.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.Platform;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
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
     * Clean profile as it's big enough
     * => profile deleted
     * => existing process killed
     * => browser started and stopped
     * => default profile copied to "profiles/CHROME/RELEASE"
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeDefault() throws Exception {
        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);

            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(150000001L); // profile will be cleaned
            when(osUtility.getRunningProcesses("chrome")).thenReturn(List.of(new ProcessInfo()))
                    .thenReturn(new ArrayList<>()); // a process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder));

            // check chrome has been executed and killed
            verify(osUtility, times(2)).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}));

            // check default profile has been copied to "profiles" sub-directory
            mockedFileUtils.verify(() -> FileUtils.copyDirectory(new File(profilePath), Utils.getProfilesDir().resolve("CHROME").resolve("Release").toFile()));
        }
    }

    /**
     * Don't clean profile as it's NOT big enough
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeProfileSmall() throws Exception {

        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);
            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(99999999L); // profile will be cleaned
            when(osUtility.getRunningProcesses("chrome")).thenReturn(List.of(new ProcessInfo()))
                    .thenReturn(new ArrayList<>()); // a process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder), never());

            // check chrome has been executed and killed
            verify(osUtility, never()).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}), never());
        }
    }

    /**
     * Do not clean profile if option cleanBrowserProfiles is set to false
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeProfileNotRequested() throws Exception {

        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);
            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(150000001L); // profile will be cleaned
            when(osUtility.getRunningProcesses("chrome")).thenReturn(List.of(new ProcessInfo()))
                    .thenReturn(new ArrayList<>()); // a process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2", "--cleanBrowserProfiles", "false"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder), never());

            // check chrome has been executed and killed
            verify(osUtility, never()).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}), never());
        }
    }

    /**
     * Clean profile as it's big enough
     * => profile deleted
     * => no existing process killed
     * => browser started and stopped
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeProfileNoExistingProcess() throws Exception {

        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);
            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(150000001L); // profile will be cleaned
            when(osUtility.getRunningProcesses("chrome")).thenReturn(new ArrayList<>()); // no process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check chrome has been executed and killed
            verify(osUtility).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}));
        }
    }

    /**
     * Profile does not exist => create it
     * => existing process killed
     * => browser started and stopped
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeProfileDoesNotExist() throws Exception {
        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);

            when(mockedProfileFolder.exists()).thenReturn(false);
            // profile does not exist => error thrown
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenThrow(new UncheckedIOException(new IOException()));
            when(osUtility.getRunningProcesses("chrome")).thenReturn(List.of(new ProcessInfo()))
                    .thenReturn(new ArrayList<>()); // a process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is not removed as it does not exist
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder), never());

            // check chrome has been executed and killed
            verify(osUtility, times(2)).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}));
        }
    }

    /**
     * Check we continue even if profile cannot be deleted
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeProfileNotDeleted() throws Exception {
        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);

            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(150000001L); // profile will be cleaned
            mockedFileUtils.when(() -> FileUtils.deleteDirectory(mockedProfileFolder)).thenThrow(new IOException());

            when(osUtility.getRunningProcesses("chrome")).thenReturn(new ArrayList<>()); // a process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder));

            // check chrome has been executed and killed
            verify(osUtility, never()).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}), never());
        }
    }

    /**
     * Do not kill process if devMode is true
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeDevMode() throws Exception {
        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);

            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(150000001L); // profile will be cleaned
            when(osUtility.getRunningProcesses("chrome"))
                    .thenReturn(new ArrayList<>()); // no process running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder));

            // check chrome has been executed and killed
            verify(osUtility, times(1)).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}));
        }
    }

    /**
     * If process is still running after killing, do not start browser as it won't be allowed
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeBrowserStillThere() throws Exception {
        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);

            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(150000001L); // profile will be cleaned
            when(osUtility.getRunningProcesses("chrome"))
                    .thenReturn(List.of(new ProcessInfo())); // no process running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder), never());

            // check chrome has been executed and killed
            verify(osUtility).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}), never());
        }
    }

    /**
     * Clean profile as it's big enough
     * => profile deleted
     * => existing process killed
     * => browser started and stopped
     * => default profile copied to "profiles/EDGE/RELEASE"
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningEdgeDefault() throws Exception {
        String profilePath = initBrowserInfo(BrowserType.EDGE, "edge");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);

            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(150000001L); // profile will be cleaned
            when(osUtility.getRunningProcesses("edge")).thenReturn(List.of(new ProcessInfo()))
                    .thenReturn(new ArrayList<>()); // a process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder));

            // check chrome has been executed and killed
            verify(osUtility, times(2)).killProcessByName("edge", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/edge"}));

            // check default profile has been copied to "profiles" sub-directory
            mockedFileUtils.verify(() -> FileUtils.copyDirectory(new File(profilePath), Utils.getProfilesDir().resolve("EDGE").resolve("Release").toFile()));
        }
    }

    /**
     * Check case where default profile does not exist (chrome has never been executed), so we start chrome to create it
     */
    @Test(groups = {"grid"})
    public void testProfileCleaningChromeWithoutDefaultProfile() throws Exception {
        String profilePath = initBrowserInfo(BrowserType.CHROME, "chrome");

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic<OSCommand> mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)
        ) {

            File mockedProfileFolder = getMockedProfileFolder(mockedPaths, profilePath);

            when(mockedProfileFolder.exists()).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.sizeOfDirectory(new File(profilePath))).thenReturn(9999999L); // profile will be cleaned
            when(osUtility.getRunningProcesses("chrome")).thenReturn(List.of(new ProcessInfo()))
                    .thenReturn(new ArrayList<>()); // a process is running

            LaunchConfig launchConfig = new LaunchConfig(new String[]{"node", "--max-sessions", "2"});
            BrowserManager browserManager = new BrowserManager(launchConfig).withBrowserStartupDelay(1);
            browserManager.initializeProfiles();

            // check directory is removed
            mockedFileUtils.verify(() -> FileUtils.deleteDirectory(mockedProfileFolder));

            // check chrome has been executed and killed
            verify(osUtility, times(2)).killProcessByName("chrome", true);
            mockedOsCommand.verify(() -> OSCommand.executeCommand(new String[]{"/usr/bin/chrome"}));

            // check default profile has been copied to "profiles" sub-directory
            mockedFileUtils.verify(() -> FileUtils.copyDirectory(new File(profilePath), Utils.getProfilesDir().resolve("CHROME").resolve("Release").toFile()));
        }
    }

    private static File getMockedProfileFolder(MockedStatic<Paths> mockedPaths, String profilePath) {
        Path mockedPath = mock(Path.class);
        File mockedProfileFolder = mock(File.class);
        mockedPaths.when(() -> Paths.get(profilePath)).thenReturn(mockedPath);
        when(mockedPath.toFile()).thenReturn(mockedProfileFolder);
        when(mockedPath.resolve(anyString())).thenReturn(mockedPath);
        return mockedProfileFolder;
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
