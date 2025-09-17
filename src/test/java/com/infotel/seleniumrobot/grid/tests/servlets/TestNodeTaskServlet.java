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
package com.infotel.seleniumrobot.grid.tests.servlets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.infotel.seleniumrobot.grid.servlets.server.GridServlet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.infotel.seleniumrobot.grid.tasks.CommandTask;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.video.DisplayRunningStepTask;
import com.infotel.seleniumrobot.grid.tasks.video.StartVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tasks.video.StopVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;
import com.seleniumtests.util.video.VideoRecorder;
import com.sun.jna.platform.win32.Advapi32Util;

import kong.unirest.UnirestException;


public class TestNodeTaskServlet extends BaseServletTest {
    
    @Mock
    OSUtility osUtility;

    @Mock
    LaunchConfig launchConfig;

    @Mock
    GridNodeConfiguration gridNodeConfiguration;
    
    @Mock
    BaseServerOptions serverOptions;
    
    @Mock
    Proxy proxy;

	@Mock
	CommandTask commandTask;

	private MockedStatic mockedLaunchConfig;
	private MockedStatic mockedAdvapi;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
		mockedLaunchConfig = mockStatic(LaunchConfig.class);
		mockedAdvapi = mockStatic(Advapi32Util.class);

		mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
		mockedLaunchConfig.when(() -> launchConfig.getExternalProgramWhiteList()).thenReturn(Arrays.asList("echo"));
		mockedLaunchConfig.when(() -> LaunchConfig.getCurrentNodeConfig()).thenReturn(gridNodeConfiguration);
    	when(gridNodeConfiguration.getServerOptions()).thenReturn(serverOptions);
    	when(serverOptions.getHostname()).thenReturn(Optional.of("127.0.0.1"));
    	when(serverOptions.getPort()).thenReturn(4444);

        VideoCaptureTask.resetVideoRecorders();
    }

    @AfterMethod(groups={"grid"}, alwaysRun = true)
    public void closeMocks() throws Exception {
		mockedLaunchConfig.close();
		mockedAdvapi.close();
    }

    @Test(groups={"grid"})
    public void killProcess() throws Exception {
		try (MockedConstruction mockedKillTask = mockConstruction(KillTask.class, (killTask, context) -> {
				when(killTask.withName(anyString())).thenReturn(killTask);
				when(killTask.withPid(anyLong())).thenReturn(killTask);
			})
		) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"kill"}, 
					"process", new String[]{"myProcess"}), 
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			Assert.assertEquals(response.message, "process killed");
			verify((KillTask)mockedKillTask.constructed().get(0)).withName("myProcess");
			verify((KillTask)mockedKillTask.constructed().get(0)).execute();
		}
    }
    
    @Test(groups={"grid"})
    public void killProcessWithError() throws Exception {
		try (MockedConstruction mockedKillTask = mockConstruction(KillTask.class, (killTask, context) -> {
			when(killTask.withName(anyString())).thenReturn(killTask);
			when(killTask.withPid(anyLong())).thenReturn(killTask);
			doThrow(Exception.class).when(killTask).execute();
		})
		) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"kill"},
					"process", new String[]{"myProcess"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
			verify((KillTask) mockedKillTask.constructed().get(0)).withName("myProcess");
			verify((KillTask) mockedKillTask.constructed().get(0)).execute();
		}
    }
    
    @Test(groups={"grid"})
    public void killPid() throws Exception {
		try (MockedConstruction mockedKillTask = mockConstruction(KillTask.class, (killTask, context) -> {
			when(killTask.withName(anyString())).thenReturn(killTask);
			when(killTask.withPid(anyLong())).thenReturn(killTask);
		})
		) {
			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"killPid"},
					"pid", new String[]{"100"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			verify((KillTask) mockedKillTask.constructed().get(0)).withPid(100L);
			verify((KillTask) mockedKillTask.constructed().get(0)).execute();
		}
    }

    @Test(groups={"grid"})
    public void killPidWithError() throws Exception {
		try (MockedConstruction mockedKillTask = mockConstruction(KillTask.class, (killTask, context) -> {
			when(killTask.withName(anyString())).thenReturn(killTask);
			when(killTask.withPid(anyLong())).thenReturn(killTask);
			doThrow(Exception.class).when(killTask).execute();
		})
		) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"killPid"},
					"pid", new String[]{"100"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
			verify((KillTask) mockedKillTask.constructed().get(0)).withPid(100L);
			verify((KillTask) mockedKillTask.constructed().get(0)).execute();
		}
    }
    
    
    @Test(groups={"grid"})
    public void getProcessList() throws UnirestException {
    	
    	ProcessInfo p1 = new ProcessInfo();
    	p1.setPid("1000");
    	ProcessInfo p2 = new ProcessInfo();
    	p2.setPid("2000");

		try (MockedStatic mockedOSUtilityFactory = mockStatic(OSUtilityFactory.class)) {
			mockedOSUtilityFactory.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);
			when(osUtility.getRunningProcesses("myProcess")).thenReturn(Arrays.asList(p1, p2));

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"processList"},
					"name", new String[]{"myProcess"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			Assert.assertEquals(response.message, "1000,2000");
		}
    }
    
    @Test(groups={"grid"})
    public void getProcessListWithError() throws UnirestException {
    	
    	ProcessInfo p1 = new ProcessInfo();
    	p1.setPid("1000");
    	ProcessInfo p2 = new ProcessInfo();
    	p2.setPid("2000");

		try (MockedStatic mockedOSUtilityFactory = mockStatic(OSUtilityFactory.class)) {
			mockedOSUtilityFactory.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);
			when(osUtility.getRunningProcesses("myProcess")).thenThrow(new RuntimeException("error on pid"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"processList"},
					"name", new String[]{"myProcess"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
			Assert.assertTrue(response.message.contains("error on pid"));
		}
    	
    }
    
    @Test(groups={"grid"})
    public void getVersion() throws UnirestException {
    	GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
    			"action", new String[]{"version"}),
				"text/html", InputStream.nullInputStream());
    	
    	Assert.assertEquals(response.httpCode, 200);
    	
    	JSONObject reply = new JSONObject(response.message);
    	Assert.assertEquals(reply.getString("version"), Utils.getCurrentversion());
    }
    
    @Test(groups={"grid"})
    public void mouseCoordinates() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.getMouseCoordinates(DriverMode.LOCAL, null)).thenReturn(new Point(2, 3));

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"mouseCoordinates"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.message, "2,3");
			Assert.assertEquals(response.httpCode, 200);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.getMouseCoordinates(eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void mouseCoordinatesWithError() throws Exception {

		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.getMouseCoordinates(DriverMode.LOCAL, null)).thenThrow(new WebDriverException("driver"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"mouseCoordinates"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.getMouseCoordinates(eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void leftClick() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"leftClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"}),
					"text/html", InputStream.nullInputStream());


			Assert.assertEquals(response.httpCode, 200);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(false), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void leftClickOnMainScreen() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"leftClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"},
					"onlyMainScreen", new String[]{"true"}),
					"text/html", InputStream.nullInputStream());


			Assert.assertEquals(response.httpCode, 200);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(true), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void leftClickWithError() throws Exception {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.leftClicOnDesktopAt( false, 0, 0, DriverMode.LOCAL, null)).thenThrow(new WebDriverException("driver"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"leftClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(false), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void doubleClick() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"doubleClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(false), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void doubleClickOnMainScreen() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"doubleClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"},
					"onlyMainScreen", new String[]{"true"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(true), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }

    @Test(groups={"grid"})
    public void doubleClickWithError() throws Exception {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.doubleClickOnDesktopAt( false, 0, 0, DriverMode.LOCAL, null)).thenThrow(new WebDriverException("driver"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"doubleClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(false), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void rightClick() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"rightClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"}),
					"text/html", InputStream.nullInputStream());

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(false), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void rightClickOnMainScreen() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"rightClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"},
					"onlyMainScreen", new String[]{"true"}),
					"text/html", InputStream.nullInputStream());

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(true), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }

    @Test(groups={"grid"})
    public void rightClickWithError() throws Exception {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.rightClicOnDesktopAt(false, 0, 0, DriverMode.LOCAL, null)).thenThrow(new WebDriverException("driver"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"rightClick"},
					"x", new String[]{"0"},
					"y", new String[]{"0"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(false), eq(0), eq(0), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void sendKeys() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"sendKeys"},
					"keycodes", new String[]{"10,20,30"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.sendKeysToDesktop(eq(Arrays.asList(10, 20, 30)), eq(DriverMode.LOCAL), isNull()));
		}
    }

    @Test(groups={"grid"})
    public void sendKeysWithError() throws Exception {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.sendKeysToDesktop(eq(Arrays.asList(10, 20, 30)), eq(DriverMode.LOCAL), isNull())).thenThrow(new WebDriverException("driver"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"sendKeys"},
					"keycodes", new String[]{"10,20,30"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.sendKeysToDesktop(eq(Arrays.asList(10, 20, 30)), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    /**
     * Check error in command is sent back to client
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void executeCommandInError() throws UnirestException {
		try (MockedStatic mockedCommandTask = mockStatic(CommandTask.class)) {
			mockedCommandTask.when(() -> CommandTask.getInstance()).thenReturn(commandTask);
			doThrow(new TaskException("Error")).when(commandTask).execute();

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"command"},
					"name", new String[]{"eco"},
					"arg0", new String[]{"hello"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
		}
    }
    
    @Test(groups={"grid"})
    public void executeCommand() throws UnirestException {
		try (MockedStatic mockedCommandTask = mockStatic(CommandTask.class)) {
			mockedCommandTask.when(() -> CommandTask.getInstance()).thenReturn(commandTask);
			when(commandTask.getResult()).thenReturn("hello guy");

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"command"},
					"name", new String[]{"echo"},
					"arg0", new String[]{"hello"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			Assert.assertEquals(response.message, "hello guy");

			verify(commandTask).setCommand("echo", Arrays.asList("hello"), null);
			verify(commandTask).execute();
		}
    }
    
    /**
     * check that a long command will keep node alive above standard timeout
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void executeCommandKeepAlive() throws UnirestException {
		try (MockedStatic mockedCommandTask = mockStatic(CommandTask.class)) {
			mockedCommandTask.when(() -> CommandTask.getInstance()).thenReturn(commandTask);
			when(commandTask.getResult()).thenReturn("hello guy");

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"command"},
					"name", new String[]{"echo"},
					"arg0", new String[]{"hello"},
					"session", new String[]{"1234"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			Assert.assertEquals(response.message, "hello guy");

			verify(commandTask).setCommand("echo", Arrays.asList("hello"), null);
			verify(commandTask).execute();
		}
    }
    
    @Test(groups={"grid"})
    public void executeCommandWithTimeout() throws UnirestException {
		try (MockedStatic mockedCommandTask = mockStatic(CommandTask.class)) {
			mockedCommandTask.when(() -> CommandTask.getInstance()).thenReturn(commandTask);
			when(commandTask.getResult()).thenReturn("hello guy");

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"command"},
					"name", new String[]{"echo"},
					"arg0", new String[]{"hello"},
					"timeout", new String[]{"40"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			Assert.assertEquals(response.message, "hello guy");

			verify(commandTask).setCommand("echo", Arrays.asList("hello"), 40);
			verify(commandTask).execute();
		}
    }
    
    @Test(groups={"grid"})
    public void writeText() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"writeText"},
					"text", new String[]{"foobar"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.writeToDesktop(eq("foobar"), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void writeTextWithError() throws Exception {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.writeToDesktop(eq("foobar"), eq(DriverMode.LOCAL), isNull())).thenThrow(new WebDriverException("driver"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"writeText"},
					"text", new String[]{"foobar"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.writeToDesktop(eq("foobar"), eq(DriverMode.LOCAL), isNull()));
		}
    }
    
    @Test(groups={"grid"})
    public void displayRunningStep() throws Exception {
		try (MockedConstruction mockedRunningStepTask = mockConstruction(DisplayRunningStepTask.class)) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"displayRunningStep"},
					"stepName", new String[]{"foobar"},
					"session", new String[]{"1234"}),
					"text/html", InputStream.nullInputStream());

			verify((DisplayRunningStepTask)mockedRunningStepTask.constructed().get(0)).execute();

			Assert.assertEquals(response.httpCode, 200);
		}
    }
    
    @Test(groups={"grid"})
    public void displayRunningStepWithError() throws Exception {
		try (MockedConstruction mockedRunningStepTask = mockConstruction(DisplayRunningStepTask.class, (runningStepTask, context) -> {
			when(runningStepTask.execute()).thenThrow(new WebDriverException("driver error"));
		})) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"displayRunningStep"},
					"stepName", new String[]{"foobar"},
					"session", new String[]{"1234"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
		}
    }

    /**
     * New way of uploading file, through body
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void uploadFile() throws UnirestException, IOException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {

			String b64png = Base64.getEncoder().encodeToString(IOUtils.toByteArray(TestNodeTaskServlet.class.getClassLoader().getResourceAsStream("upload.png")));

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"uploadFile"},
					"name", new String[]{"foobar.txt"}),
					MediaType.OCTET_STREAM.toString(),
					Thread.currentThread().getContextClassLoader().getResourceAsStream("upload.png"));

			Assert.assertEquals(response.httpCode, 200);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq(b64png), eq(DriverMode.LOCAL), isNull()));
		}
    }

	@Test(groups={"grid"})
	public void uploadFileWithError() throws Exception {
		String b64png = Base64.getEncoder().encodeToString(IOUtils.toByteArray(TestNodeTaskServlet.class.getClassLoader().getResourceAsStream("upload.png")));

		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq(b64png), eq(DriverMode.LOCAL), isNull())).thenThrow(new WebDriverException("driver"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
							"action", new String[]{"uploadFile"},
							"name", new String[]{"foobar.txt"}),
					MediaType.OCTET_STREAM.toString(),
					Thread.currentThread().getContextClassLoader().getResourceAsStream("upload.png"));

			Assert.assertEquals(response.httpCode, 500);

			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq(b64png), eq(DriverMode.LOCAL), isNull()));
		}
	}
    
    @Test(groups={"grid"})
    public void captureDesktop() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			when(CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull())).thenReturn("ABCDEF");

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"screenshot"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull()));
			Assert.assertEquals(response.message, "ABCDEF");
		}
    }
    
    @Test(groups={"grid"})
    public void captureDesktopWithError() throws UnirestException {
		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
			when(CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull())).thenThrow(new WebDriverException("capture"));

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"screenshot"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
			mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull()));
			Assert.assertTrue(response.message.contains("capture"));
		}
    }
    
    @Test(groups={"grid"})
    public void startCapture() throws Exception {
		try (MockedConstruction mockedStartCaptureTask = mockConstruction(StartVideoCaptureTask.class)) {
			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"startVideoCapture"},
					"session", new String[]{"1234567890-4"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 200);
			verify((StartVideoCaptureTask)mockedStartCaptureTask.constructed().get(0)).execute();
		}
    }
    
    @Test(groups={"grid"})
    public void startCaptureWithError() throws Exception {
		try (MockedConstruction mockedStartCaptureTask = mockConstruction(StartVideoCaptureTask.class, (startCaptureTask, context) -> {
				when(startCaptureTask.execute()).thenThrow(new WebDriverException("recorder"));
			}))	{

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"startVideoCapture"},
					"session", new String[]{"1234567890-4"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
			Assert.assertTrue(response.message.contains("recorder"));
		}
    }
    
    /**
     * Start and stop capture. Check file is written
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCapture() throws UnirestException, IOException {
		File tempVideo = File.createTempFile("video-", ".avi");
		FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);

		try (MockedConstruction mockedStopCaptureTask = mockConstruction(StopVideoCaptureTask.class, (stopCaptureTask, context) -> {
			when(stopCaptureTask.getVideoFile()).thenReturn(tempVideo);
			when(stopCaptureTask.execute()).thenReturn(stopCaptureTask);
		})) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"stopVideoCapture"},
					"session", new String[]{"1234567890-2"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(FileUtils.readFileToString(response.file, StandardCharsets.UTF_8), "foo");
			Assert.assertEquals(response.contentType.toString(), "video/x-msvideo");

			// check video file has not been deleted
			Assert.assertTrue(tempVideo.exists());
		}
    }

	@Test(groups={"grid"})
    public void stopCaptureMp4() throws UnirestException, IOException {
		File tempVideo = File.createTempFile("video-", ".mp4");
		FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);

		try (MockedConstruction mockedStopCaptureTask = mockConstruction(StopVideoCaptureTask.class, (stopCaptureTask, context) -> {
			when(stopCaptureTask.getVideoFile()).thenReturn(tempVideo);
			when(stopCaptureTask.execute()).thenReturn(stopCaptureTask);
		})) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"stopVideoCapture"},
					"session", new String[]{"1234567890-2"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(FileUtils.readFileToString(response.file, StandardCharsets.UTF_8), "foo");
			Assert.assertEquals(response.contentType.toString(), "video/mp4");

			// check video file has not been deleted
			Assert.assertTrue(tempVideo.exists());
		}
    }

    /**
     * Start and stop capture. Check file is written
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCaptureWithError() throws UnirestException, IOException {

    	
    	File tempVideo = File.createTempFile("video-", ".avi");
    	FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);

		try (MockedConstruction mockedStopCaptureTask = mockConstruction(StopVideoCaptureTask.class, (stopCaptureTask, context) -> {
			when(stopCaptureTask.getVideoFile()).thenThrow(new SeleniumGridException("error stopping"));
			when(stopCaptureTask.execute()).thenReturn(stopCaptureTask);
		})) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"stopVideoCapture"},
					"session", new String[]{"1234567890-2"}),
					"text/html", InputStream.nullInputStream());


			Assert.assertEquals(response.httpCode, 500);
			Assert.assertTrue(response.message.contains("stop"));
			Assert.assertNull(VideoCaptureTask.getVideoRecorders().get("1234567890-2"));
		}
    }
    
    /**
     * Check when no file has been captured. Data returned is empty
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCaptureNoFile() throws UnirestException, IOException {

		try (MockedConstruction mockedStopCaptureTask = mockConstruction(StopVideoCaptureTask.class, (stopCaptureTask, context) -> {
			when(stopCaptureTask.getVideoFile()).thenReturn(null);
			when(stopCaptureTask.execute()).thenReturn(stopCaptureTask);
		})) {

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"stopVideoCapture"},
					"session", new String[]{"12345678901"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertNull(VideoCaptureTask.getVideoRecorders().get("12345678901"));
			Assert.assertNull(response.file);
		}
    }
    
    @Test(groups={"grid"})
    public void stopCaptureWithoutStart() throws UnirestException, IOException {

		File tempVideo = File.createTempFile("video-", ".avi");
		FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);

		try (MockedStatic mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class);
			 ) {

			mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenReturn(tempVideo);

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"stopVideoCapture"},
					"session", new String[]{"12345678902"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertNull(VideoCaptureTask.getVideoRecorders().get("12345678902"));
			Assert.assertNull(response.file);
		}
    }
    
    @Test(groups={"grid"})
    public void driverPids() throws Exception {

		try (MockedStatic mockedOsUtility = mockStatic(OSUtility.class)) {
			mockedOsUtility.when(() -> OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);
			BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));

			Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
			browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
			browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));

			mockedOsUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
			doReturn(Arrays.asList(200L)).when(bi1).getDriverAndBrowserPid(anyList());

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"driverPids"},
					"existingPids", new String[]{"100,300"},
					"browserName", new String[]{"chrome"},
					"browserVersion", new String[]{"85.0"}),
					"text/html", InputStream.nullInputStream());


			Assert.assertEquals(response.httpCode, 200);
			Assert.assertEquals(response.message, "200");
		}
    }
    
    @Test(groups={"grid"})
    public void driverPidsWithError() throws Exception {
    	
    	BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));
		try (MockedStatic mockedOsUtility = mockStatic(OSUtility.class)) {
			mockedOsUtility.when(() -> OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);

			Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
			browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
			browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));

			mockedOsUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
			doThrow(new RuntimeException("pids")).when(bi1).getDriverAndBrowserPid(anyList());

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"driverPids"},
					"existingPids", new String[]{"100,300"},
					"browserName", new String[]{"chrome"},
					"browserVersion", new String[]{"85.0"}),
					"text/html", InputStream.nullInputStream());


			Assert.assertEquals(response.httpCode, 500);
			Assert.assertTrue(response.message.contains("pids"));
		}
    }
    
    @Test(groups={"grid"})
    public void browserAndDriverPids() throws Exception {
    	
    	BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));
		try (MockedStatic mockedOsUtility = mockStatic(OSUtility.class)) {
			mockedOsUtility.when(() -> OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);

			Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
			browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
			browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));

			mockedOsUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
			doReturn(Arrays.asList(200L, 400L)).when(bi1).getAllBrowserSubprocessPids(Arrays.asList(100L));

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"browserAndDriverPids"},
					"parentPids", new String[]{"100"},
					"browserName", new String[]{"chrome"},
					"browserVersion", new String[]{"85.0"}),
					"text/html", InputStream.nullInputStream());


			Assert.assertEquals(response.httpCode, 200);
			Assert.assertEquals(response.message, "200,400");
		}
    }
    

    @Test(groups={"grid"})
    public void browserAndDriverPidsWithError() throws Exception {
    	
    	BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));
		try (MockedStatic mockedOsUtility = mockStatic(OSUtility.class)) {
			mockedOsUtility.when(() -> OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);

			Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
			browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
			browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));

			mockedOsUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
			doThrow(new RuntimeException("pids")).when(bi1).getAllBrowserSubprocessPids(Arrays.asList(100L));

			GridServlet.ServletResponse response = new NodeTaskServlet().executeGetAction(Map.of(
					"action", new String[]{"browserAndDriverPids"},
					"parentPids", new String[]{"100"},
					"browserName", new String[]{"chrome"},
					"browserVersion", new String[]{"85.0"}),
					"text/html", InputStream.nullInputStream());

			Assert.assertEquals(response.httpCode, 500);
			Assert.assertTrue(response.message.contains("pids"));
		}
    }

    /**
     * test we kill browsers and drivers when devMode is disabled
     * @throws UnirestException
     * @throws IOException 
     */
    @Test(groups={"grid"})
    public void cleanNode() throws UnirestException, IOException {
		try (MockedStatic mockedOsUtility = mockStatic(OSUtilityFactory.class);
			MockedStatic mockedFileUtils = mockStatic(FileUtils.class)
		) {
			mockedOsUtility.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);

			mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
			when(launchConfig.getDevMode()).thenReturn(false);

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"clean"}),
					"text/html", InputStream.nullInputStream());

			verify(osUtility).killAllWebBrowserProcess(true);
			verify(osUtility).killAllWebDriverProcess();

			mockedFileUtils.verify(() -> FileUtils.cleanDirectory(any(File.class)));
		}
    }
    
    @Test(groups={"grid"})
    public void cleanNodeResetWindowsProxy() throws UnirestException, IOException {
		try (MockedStatic mockedOsUtility = mockStatic(OSUtilityFactory.class);
			 MockedStatic mockedFileUtils = mockStatic(FileUtils.class)
		) {
			mockedOsUtility.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);

			mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
			when(launchConfig.getDevMode()).thenReturn(false);
			when(launchConfig.getProxyConfig()).thenReturn(proxy);

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"clean"}),
					"text/html", InputStream.nullInputStream());

			// check proxy is reset
			verify(osUtility).setSystemProxy(proxy);
		}
    }
    
    @Test(groups={"grid"})
    public void cleanNodeDoNotResetWindowsProxy2() throws UnirestException, IOException {
		try (MockedStatic mockedOsUtility = mockStatic(OSUtilityFactory.class);
			 MockedStatic mockedFileUtils = mockStatic(FileUtils.class)
		) {

			mockedOsUtility.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);

			mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
			when(launchConfig.getDevMode()).thenReturn(false);
			when(launchConfig.getProxyConfig()).thenReturn(null);

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"clean"}),
					"text/html", InputStream.nullInputStream());

			// check proxy is reset
			verify(osUtility, never()).setSystemProxy(proxy);
		}
    }
    
    /**
     * Test videos younger than 8 hours are not deleted
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void cleanNodeDoNotPurgeNewVideo() throws UnirestException, IOException {

    	// create video file
    	File videoFile = Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER, "video.mp4").toFile();
    	FileUtils.write(videoFile, "foo", StandardCharsets.UTF_8);
    	Files.setAttribute(videoFile.toPath(), "lastModifiedTime", FileTime.fromMillis(videoFile.lastModified() - 7 * 3600000));

		try (MockedStatic mockedOsUtility = mockStatic(OSUtilityFactory.class);
			 MockedStatic mockedFileUtils = mockStatic(FileUtils.class)
		) {
			mockedOsUtility.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);

			mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
	    	when(launchConfig.getDevMode()).thenReturn(false);

	    	GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
	    			"action", new String[]{"clean"}),
					"text/html", InputStream.nullInputStream());
	    	
	    	Assert.assertTrue(videoFile.exists());
	    	
    	} finally {
    		FileUtils.deleteDirectory(videoFile.getParentFile());
    	}
    	
    }
    
    /**
     * Test videos older than 8 hours are deleted
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void cleanNodePurgeOldVideo() throws UnirestException, IOException {
    	
    	// create video file
    	File videoFile = Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER, "video.mp4").toFile();
    	FileUtils.write(videoFile, "foo", StandardCharsets.UTF_8);
    	Files.setAttribute(videoFile.toPath(), "lastModifiedTime", FileTime.fromMillis(videoFile.lastModified() - 9 * 3600000));

		try (MockedStatic mockedOsUtility = mockStatic(OSUtilityFactory.class);
			 MockedStatic mockedFileUtils = mockStatic(FileUtils.class)
		) {
			mockedOsUtility.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);

			mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    		when(launchConfig.getDevMode()).thenReturn(false);

    		GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
    			"action", new String[]{"clean"}),
					"text/html", InputStream.nullInputStream());
    		
    		// file should have been deleted
    		Assert.assertFalse(videoFile.exists());
    		
    	} finally {
    		FileUtils.deleteDirectory(videoFile.getParentFile());
    	}
    	
    }
    
    /**
     * test we do not kill browsers and drivers when devMode is enabled
     * @throws UnirestException
     * @throws IOException 
     */
    @Test(groups={"grid"})
    public void doNotCleanNode() throws UnirestException, IOException {
		try (MockedStatic mockedOsUtility = mockStatic(OSUtilityFactory.class);
			 MockedStatic mockedFileUtils = mockStatic(FileUtils.class)
		) {
			mockedOsUtility.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);

			mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
			when(launchConfig.getDevMode()).thenReturn(true);

			GridServlet.ServletResponse response = new NodeTaskServlet().executePostAction(Map.of(
					"action", new String[]{"clean"}),
					"text/html", InputStream.nullInputStream());

			verify(osUtility, never()).killAllWebBrowserProcess(true);
			verify(osUtility, never()).killAllWebDriverProcess();

			mockedFileUtils.verify(() -> FileUtils.cleanDirectory(any(File.class)), never());
		}
    }
}
