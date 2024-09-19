package com.infotel.seleniumrobot.grid.tests.mobile;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.infotel.seleniumrobot.grid.mobile.LocalAppiumLauncher;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
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

import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.customexception.ScenarioException;
import com.seleniumtests.util.osutility.OSCommand;

public class TestLocalAppiumLauncher extends BaseMockitoTest {

    @Mock
    Process nodeProcess;

    @Mock
    Path nodePath;

    @Mock
    File nodeFile;

    private MockedStatic mockedFileUtils;
    private MockedStatic mockedOSUtility;
    private MockedStatic mockedSystem;
    private MockedStatic mockedOsCommand;

    private void initValidAppiumInstallation() throws IOException {
        mockedFileUtils = mockStatic(FileUtils.class);
        mockedSystem = mockStatic(SystemUtility.class);
        mockedOSUtility = mockStatic(OSUtility.class);
        mockedSystem.when(() -> SystemUtility.getenv("APPIUM_PATH")).thenReturn("/opt/appium/");
        mockedFileUtils.when(() -> FileUtils.readFileToString(new File("/opt/appium/node_modules/appium/package.json"), StandardCharsets.UTF_8))
                .thenReturn("{\"name\":\"appium\",\"version\":\"2.4.13\"}");
        mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(true);

    }

    private void initValidNodeInstallation() {
        mockedOsCommand = mockStatic(OSCommand.class);
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("C:\\nodejs\\node.exe -v")).thenReturn("v6.2.1");
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
    @Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
    public void testAppiumNotFound() {
        try (MockedStatic mockedSystem = mockStatic(SystemUtility.class);) {
            mockedSystem.when(() -> SystemUtility.getenv("APPIUM_PATH")).thenReturn(null);
            new LocalAppiumLauncher();
        }
    }

    /**
     * Test when appium_home exist, version found
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void testAppiumFound() throws IOException {
        try (MockedConstruction mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
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
     * @throws IOException
     */
    @Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
    public void testAppiumFoundInvalid() throws IOException {
        try (MockedStatic mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic mockedSystem = mockStatic(SystemUtility.class);
             MockedConstruction mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
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
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void testNodeFoundInSystemPath() throws IOException {
        initValidAppiumInstallation();

        try (MockedStatic mockedOsCommand = mockStatic(OSCommand.class)) {
            mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("v6.2.1");

            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            Assert.assertEquals(appium.getNodeVersion(), "v6.2.1");
        }
    }

    @Test(groups={"grid"})
    public void testNodeFoundInSystemPathWindows() throws IOException {
        initValidAppiumInstallation();

        try (MockedStatic mockedOsCommand = mockStatic(OSCommand.class);
             MockedConstruction mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
                 when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
             }))) {
            mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(true);
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("C:\\nodejs\\node.exe -v")).thenReturn("v6.2.1");

            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            Assert.assertEquals(appium.getNodeVersion(), "v6.2.1");
        }
    }

    /**
     * Test when node is not found in system path, an error is raised
     * @throws IOException
     */
    @Test(groups={"grid"}, expectedExceptions=ScenarioException.class)
    public void testNodeNotFoundInPathWindows() throws IOException {
        initValidAppiumInstallation();

        try (MockedStatic mockedOsCommand = mockStatic(OSCommand.class);
             MockedConstruction mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
                 when(mock.searchInWindowsPath("node")).thenThrow(new ScenarioException("Program node not found in path"));
             }))) {
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("node command not found");
            new LocalAppiumLauncher();
        }
    }

    /**
     * Test when node is not found in system path, an error is raised
     * @throws IOException
     */
    @Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
    public void testNodeNotFoundInPath() throws IOException {
        initValidAppiumInstallation();

        try (MockedStatic mockedOsCommand = mockStatic(OSCommand.class);
             ) {
            mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("node command not found");
            new LocalAppiumLauncher();
        }
    }

    @Test(groups={"grid"})
    public void testAppiumStartup() throws IOException {
        initValidAppiumInstallation();
        initValidNodeInstallation();
        mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(true);

        // this way, we will check if, on windows, the full node path is surrounded by quotes
        mockedOsCommand.when(() -> OSCommand.executeCommand("cmd /c start /MIN cmd /C \"C:\\nodejs\\node.exe\" /opt/appium//node_modules/appium/index.js --port 4723 ")).thenReturn(nodeProcess);

        try (MockedConstruction mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
                 when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
             }))) {
            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            appium.setAppiumPort(4723);
            appium.startAppiumWithoutWait();

            Assert.assertEquals(appium.getAppiumProcess(), nodeProcess);
        }
    }

    @Test(groups={"grid"}, expectedExceptions=ScenarioException.class)
    public void testAppiumStopWithoutStart() throws IOException {
        initValidAppiumInstallation();
        initValidNodeInstallation();
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("v6.2.1");
        mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);

        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        appium.stopAppium();
    }

    @Test(groups={"grid"})
    public void testAppiumStop() throws IOException {

        initValidAppiumInstallation();
        initValidNodeInstallation();
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("v6.2.1");
        mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);

        mockedOsCommand.when(() -> OSCommand.executeCommand(contains("node_modules/appium/"))).thenReturn(nodeProcess);

        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        appium.setAppiumPort(4723);
        appium.startAppiumWithoutWait();
        appium.stopAppium();
        Mockito.verify(nodeProcess).destroy();
    }

    @Test(groups={"grid"})
    public void testAppiumRandomPort() throws IOException {
        initValidAppiumInstallation();
        initValidNodeInstallation();
        try (MockedConstruction mockedNewOsCommand = mockConstruction(OSCommand.class, ((mock, context) -> {
            when(mock.searchInWindowsPath("node")).thenReturn("C:\\nodejs\\node.exe");
        }))) {
            LocalAppiumLauncher appium1 = new LocalAppiumLauncher();
            LocalAppiumLauncher appium2 = new LocalAppiumLauncher();
            Assert.assertNotEquals(appium1.getAppiumPort(), appium2.getAppiumPort());
        }
    }
}