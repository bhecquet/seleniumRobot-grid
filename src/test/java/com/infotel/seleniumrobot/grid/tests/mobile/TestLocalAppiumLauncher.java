package com.infotel.seleniumrobot.grid.tests.mobile;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.contains;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.infotel.seleniumrobot.grid.mobile.LocalAppiumLauncher;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.util.osutility.SystemUtility;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
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
    private MockedStatic mockedSystem;
    private MockedStatic mockedOsCommand;

    private void initValidAppiumInstallation() throws IOException {
        mockedFileUtils = mockStatic(FileUtils.class);
        mockedSystem = mockStatic(SystemUtility.class);
        mockedSystem.when(() -> SystemUtility.getenv("APPIUM_HOME")).thenReturn("/opt/appium/");
        mockedFileUtils.when(() -> FileUtils.readFileToString(new File("/opt/appium/node_modules/appium/package.json"), StandardCharsets.UTF_8))
                .thenReturn("{\"name\":\"appium\",\"version\":\"1.4.13\"}");

    }

    private void initValidNodeInstallation() {
        mockedOsCommand = mockStatic(OSCommand.class);
        mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("v6.2.1");
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
    }

    /**
     * Test when appium home does not exist, an error is raised
     */
    @Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
    public void testAppiumNotFound() {
        try (MockedStatic mockedSystem = mockStatic(SystemUtility.class);) {
            mockedSystem.when(() -> SystemUtility.getenv("APPIUM_HOME")).thenReturn(null);
            new LocalAppiumLauncher();
        }
    }

    /**
     * Test when appium_home exist, version found
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void testAppiumFound() throws IOException {
        initValidAppiumInstallation();
        initValidNodeInstallation();
        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        Assert.assertEquals(appium.getAppiumVersion(), "1.4.13");
    }


    /**
     * Test when appium_home path does not contain a right appiumConfig file
     * @throws IOException
     */
    @Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
    public void testAppiumFoundInvalid() throws IOException {
        try (MockedStatic mockedFileUtils = mockStatic(FileUtils.class);
             MockedStatic mockedSystem = mockStatic(SystemUtility.class)) {
            mockedSystem.when(() -> SystemUtility.getenv("APPIUM_HOME")).thenReturn("/opt/appium/");
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
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("v6.2.1");

            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            Assert.assertEquals(appium.getNodeVersion(), "v6.2.1");
        }
    }

    /**
     * Test when node is not found in system path, an error is raised
     * @throws IOException
     */
    @Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
    public void testNodeNotFoundInPath() throws IOException {
        initValidAppiumInstallation();

        try (MockedStatic mockedOsCommand = mockStatic(OSCommand.class)) {
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("node -v")).thenReturn("node command not found");
            new LocalAppiumLauncher();
        }
    }

    /**
     * !!! THIS METHOD is ignored as PowerMock cannot mock nio.Paths class !!! 
     *
     * Test when node is found in appium path
     * @throws IOException
     */
    @Test(groups={"grid"}, enabled=false)
    public void testNodeFoundInAppiumPath() throws IOException {
        initValidAppiumInstallation();

        try (MockedStatic mockedOsCommand = mockStatic(OSCommand.class);
             MockedStatic mockedPaths = mockStatic(Paths.class)) {
            mockedOsCommand.when(() -> OSCommand.executeCommandAndWait("/opt/appium/node -v")).thenReturn("v6.2.1");


            mockedPaths.when(() -> Paths.get("/opt/appium/", "node")).thenReturn(nodePath);
            when(nodePath.toFile()).thenReturn(nodeFile);
            when(nodeFile.exists()).thenReturn(true);

            LocalAppiumLauncher appium = new LocalAppiumLauncher();
            Assert.assertEquals(appium.getNodeVersion(), "v6.2.1");
        }
    }

    @Test(groups={"grid"})
    public void testAppiumStartup() throws IOException {
        initValidAppiumInstallation();
        initValidNodeInstallation();

        mockedOsCommand.when(() -> OSCommand.executeCommand(contains("node_modules/appium/"))).thenReturn(nodeProcess);

        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        appium.setAppiumPort(4723);
        appium.startAppiumWithoutWait();

        Assert.assertEquals(appium.getAppiumProcess(), nodeProcess);
    }

    @Test(groups={"grid"}, expectedExceptions=ScenarioException.class)
    public void testAppiumStopWithoutStart() throws IOException {
        initValidAppiumInstallation();
        initValidNodeInstallation();

        LocalAppiumLauncher appium = new LocalAppiumLauncher();
        appium.stopAppium();
    }

    @Test(groups={"grid"})
    public void testAppiumStop() throws IOException {

        initValidAppiumInstallation();
        initValidNodeInstallation();

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
        LocalAppiumLauncher appium1 = new LocalAppiumLauncher();
        LocalAppiumLauncher appium2 = new LocalAppiumLauncher();
        Assert.assertNotEquals(appium1.getAppiumPort(), appium2.getAppiumPort());
    }
}