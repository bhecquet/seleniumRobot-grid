package com.infotel.seleniumrobot.grid.tests.mobile;

import com.infotel.seleniumrobot.grid.mobile.LocalAppiumLauncher;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.customexception.ScenarioException;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.SystemUtility;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class TestLocalAppiumLauncher extends BaseMockitoTest {

    @Mock
    Process nodeProcess;

    @Mock
    Path driverPath;

    @Mock
    File driverFile;

    private MockedStatic<FileUtils> mockedFileUtils;
    private MockedStatic<OSUtility> mockedOSUtility;
    private MockedStatic<SystemUtility> mockedSystem;
    private MockedStatic<OSCommand> mockedOsCommand;

    private void initValidAppiumInstallation() {
        mockedFileUtils = mockStatic(FileUtils.class);
        mockedSystem = mockStatic(SystemUtility.class);
        mockedOSUtility = mockStatic(OSUtility.class);
        mockedSystem.when(() -> SystemUtility.getenv("APPIUM_PATH")).thenReturn("/opt/appium/");
        mockedFileUtils.when(() -> FileUtils.readFileToString(new File("/opt/appium/node_modules/appium/package.json"), StandardCharsets.UTF_8))
                .thenReturn("{\"name\":\"appium\",\"version\":\"2.4.13\"}");
        mockedOSUtility.when(OSUtility::isWindows).thenReturn(true);

    }

    private void initValidNodeInstallation() {
        mockedOsCommand = mockStatic(OSCommand.class);
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(new String[]{"C:\\nodejs\\node.exe", "-v"})).thenReturn("v6.2.1");
    }

    @AfterMethod(groups = "ut", alwaysRun = true)
    private void closeMocks() {
        if (mockedFileUtils != null) {
            mockedFileUtils.close();
            mockedFileUtils = null;
        }
        if (mockedSystem != null) {
            mockedSystem.close();
            mockedSystem = null;
        }
        if (mockedOsCommand != null) {
            mockedOsCommand.close();
            mockedOsCommand = null;
        }
        if (mockedOSUtility != null) {
            mockedOSUtility.close();
            mockedOSUtility = null;
        }
    }

    /**
     * Test when appium home does not exist, an error is raised
     */
    @Test(groups = {"grid"}, expectedExceptions = ConfigurationException.class)
    public void testAppiumNotFound() {
        try (MockedStatic<SystemUtility> newMockedSystem = mockStatic(SystemUtility.class);) {
            newMockedSystem.when(() -> SystemUtility.getenv("APPIUM_PATH")).thenReturn(null);
            new LocalAppiumLauncher();
        }
    }

    /**
     * Test when appium_home exist, version found
     */
    @Test(groups = {"grid"})
    public void testAppiumFound() {
        try (MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
        }))) {
            initValidAppiumInstallation();
            initValidNodeInstallation();
            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            Assert.assertEquals(appium.getAppiumVersion(), "2.4.13");
        }
    }


    /**
     * Test when appium_home path does not contain a right appiumConfig file
     *
     * @throws IOException
     */
    @Test(groups = {"grid"}, expectedExceptions = ConfigurationException.class)
    public void testAppiumFoundInvalid() {
        try (
                MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
                    when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
                }))) {
            mockedSystem.when(() -> SystemUtility.getenv("APPIUM_PATH")).thenReturn("/opt/appium/");
            mockedFileUtils.when(() -> FileUtils.readFileToString(new File("/opt/appium/node_modules/appium/package.json"), StandardCharsets.UTF_8))
                    .thenReturn("{\"name\":\"application\"}");
            new LocalAppiumLauncher();
        }
    }

    /**
     * Test when node is found in system path
     *
     * @throws IOException
     */
    @Test(groups = {"grid"})
    public void testNodeFoundInSystemPath() {
        initValidAppiumInstallation();

        mockedOSUtility.when(OSUtility::isWindows).thenReturn(false);
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(new String[]{"node", "-v"})).thenReturn("v6.2.1");

        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        Assert.assertEquals(appium.getNodeVersion(), "v6.2.1");

    }

    @Test(groups = {"grid"})
    public void testNodeFoundInSystemPathWindows() {
        initValidAppiumInstallation();

        try (
                MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
                    when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
                }))) {
            mockedOSUtility.when(OSUtility::isWindows).thenReturn(true);
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(new String[]{"C:\\nodejs\\node.exe", "-v"})).thenReturn("v6.2.1");

            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            Assert.assertEquals(appium.getNodeVersion(), "v6.2.1");
        }
    }

    /**
     * Test when node is not found in system path, an error is raised
     */
    @Test(groups = {"grid"}, expectedExceptions = ScenarioException.class)
    public void testNodeNotFoundInPathWindows() {
        initValidAppiumInstallation();

        try (MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenThrow(new ScenarioException("Program node not found in path"));
        }))) {
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(new String[]{"node", "-v"})).thenReturn("node command not found");
            new LocalAppiumLauncher();
        }
    }

    /**
     * Test when node is not found in system path, an error is raised
     */
    @Test(groups = {"grid"}, expectedExceptions = ConfigurationException.class)
    public void testNodeNotFoundInPath() {
        initValidAppiumInstallation();

        mockedOSUtility.when(OSUtility::isWindows).thenReturn(false);
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(new String[]{"node", "-v"})).thenReturn("node command not found");
        new LocalAppiumLauncher();

    }

    @Test(groups = {"grid"})
    public void testAppiumStartup() {
        initValidAppiumInstallation();
        initValidNodeInstallation();
        mockedOSUtility.when(OSUtility::isWindows).thenReturn(true);

        // this way, we will check if, on windows, the full node path is surrounded by quotes
        mockedOsCommand.when(() -> OSCommand.executeCommand(new String[]{"cmd", "/c", "start", "/MIN", "cmd", "/C", "\"C:\\nodejs\\node.exe\"", "/opt/appium//node_modules/appium/index.js", "--port", "4723"})).thenReturn(nodeProcess);

        try (MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
        }))) {
            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            appium.setAppiumPort(4723);
            appium.startAppiumWithoutWait();

            Assert.assertEquals(appium.getAppiumProcess(), nodeProcess);
        }
    }

    @Test(groups = {"grid"}, expectedExceptions = ScenarioException.class)
    public void testAppiumStopWithoutStart() {
        initValidAppiumInstallation();
        initValidNodeInstallation();
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(new String[]{"node", "-v"})).thenReturn("v6.2.1");
        mockedOSUtility.when(OSUtility::isWindows).thenReturn(false);

        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        appium.stopAppium();
    }

    @Test(groups = {"grid"})
    public void testAppiumStop() {

        initValidAppiumInstallation();
        initValidNodeInstallation();
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(new String[]{"node", "-v"})).thenReturn("v6.2.1");
        mockedOSUtility.when(OSUtility::isWindows).thenReturn(false);

        mockedOsCommand.when(() -> OSCommand.executeCommand(new String[]{"node_modules/appium/"})).thenReturn(nodeProcess);

        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        appium.setAppiumPort(4723);
        appium.startAppiumWithoutWait();
        appium.stopAppium();
        Mockito.verify(nodeProcess).destroy();
    }

    @Test(groups = {"grid"})
    public void testAppiumRandomPort() {
        initValidAppiumInstallation();
        initValidNodeInstallation();
        try (MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
        }))) {
            LocalAppiumLauncher appium1 = new LocalAppiumLauncher();
            LocalAppiumLauncher appium2 = new LocalAppiumLauncher();
            Assert.assertNotEquals(appium1.getAppiumPort(), appium2.getAppiumPort());
        }
    }

    @Test(groups = {"grid"})
    public void testGetDriverList() {
        try (MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
        }));
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)) {
            initValidAppiumInstallation();
            initValidNodeInstallation();
            mockedSystem.when(() -> SystemUtility.getenv("APPIUM_HOME")).thenReturn("/home/user");
            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            mockedPaths.when(() -> Paths.get("/home/user", ".appium", "node_modules")).thenReturn(driverPath);
            when(driverPath.toFile()).thenReturn(driverFile);

            File driver1 = spy(new File("appium-uiautomator2-driver"));
            when(driver1.isDirectory()).thenReturn(true);
            File driver2 = spy(new File(".cache"));
            when(driver2.isDirectory()).thenReturn(true);
            File driver3 = spy(new File("appium-flaui2-driver"));
            when(driver3.isDirectory()).thenReturn(true);
            File driver4 = spy(new File("appium-xcui-driver"));
            when(driver4.isDirectory()).thenReturn(false);
            when(driverFile.listFiles()).thenReturn(new File[]{driver1, driver2, driver3, driver4});

            Assert.assertEquals(appium.getDriverList(), List.of("uiautomator2", "flaui2"));
        }
    }

    @Test(groups = {"grid"})
    public void testGetDriverList2() {
        try (MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
        }));
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)) {
            initValidAppiumInstallation();
            initValidNodeInstallation();
            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            mockedPaths.when(() -> Paths.get(System.getProperty("user.home"), ".appium", "node_modules")).thenReturn(driverPath);
            when(driverPath.toFile()).thenReturn(driverFile);

            File driver1 = spy(new File("appium-uiautomator2-driver"));
            when(driver1.isDirectory()).thenReturn(true);
            File driver2 = spy(new File("appium-flaui2-driver"));
            when(driver2.isDirectory()).thenReturn(true);
            when(driverFile.listFiles()).thenReturn(new File[]{driver1, driver2});

            Assert.assertEquals(appium.getDriverList(), List.of("uiautomator2", "flaui2"));
        }
    }

    @Test(groups = {"grid"})
    public void testGetDriverListFolderNotPresent() {
        try (MockedConstruction<OSCommand> mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
        }));
             MockedStatic<Paths> mockedPaths = mockStatic(Paths.class, Mockito.CALLS_REAL_METHODS)) {
            initValidAppiumInstallation();
            initValidNodeInstallation();
            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            mockedPaths.when(() -> Paths.get(System.getProperty("user.home"), ".appium", "node_modules")).thenReturn(driverPath);
            when(driverPath.toFile()).thenReturn(driverFile);

            File driver1 = spy(new File("appium-uiautomator2-driver"));
            when(driver1.isDirectory()).thenReturn(true);
            File driver2 = spy(new File("appium-flaui2-driver"));
            when(driver2.isDirectory()).thenReturn(true);
            when(driverFile.listFiles()).thenReturn(null);

            Assert.assertEquals(appium.getDriverList(), new ArrayList<>());
        }
    }

}