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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.json.JSONObject;
import org.mockito.Mock;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.net.HttpHeaders;
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
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;
import com.seleniumtests.util.video.VideoRecorder;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

@PrepareForTest({CustomEventFiringWebDriver.class, OSUtilityFactory.class, OSUtility.class, BrowserInfo.class, LaunchConfig.class, FileUtils.class, OSCommand.class, NodeTaskServlet.class, CommandTask.class, Advapi32Util.class})
@PowerMockIgnore("javax.net.ssl.*") // to avoid error java.security.NoSuchAlgorithmException: class configured for SSLContext: sun.security.ssl.SSLContextImpl$TLS10Context not a SSLContext
public class TestNodeTaskServlet extends BaseServletTest {

    private Server nodeServer;
    private HttpHost serverHost;
    
    @Mock
    OSUtility osUtility;
    
    @Mock
    KillTask killTask;
    
    @Mock
    CommandTask commandTask;
    
    @Mock
    StartVideoCaptureTask startCaptureTask;
    
    @Mock
    StopVideoCaptureTask stopCaptureTask;
    
    @Mock
    DisplayRunningStepTask runningStepTask;
    
    @Mock
    VideoRecorder recorder;
    
    @Mock
    LaunchConfig launchConfig;
    
    @Mock
    LocalAppiumLauncher appiumLauncher;
    
    @Mock
    GridNodeConfiguration gridNodeConfiguration;
    
    @Mock
    BaseServerOptions serverOptions;
    
    @Mock
    Proxy proxy;

    NodeTaskServlet nodeServlet;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {

    	PowerMockito.mockStatic(LaunchConfig.class);
    	PowerMockito.mockStatic(Advapi32Util.class);
    	when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    	when(launchConfig.getExternalProgramWhiteList()).thenReturn(Arrays.asList("echo"));
    	when(LaunchConfig.getCurrentNodeConfig()).thenReturn(gridNodeConfiguration);
    	when(gridNodeConfiguration.getServerOptions()).thenReturn(serverOptions);
    	when(serverOptions.getHostname()).thenReturn(Optional.of("127.0.0.1"));
    	when(serverOptions.getPort()).thenReturn(4444);
    	
    	nodeServlet = new NodeTaskServlet();
    	
    	PowerMockito.whenNew(KillTask.class).withAnyArguments().thenReturn(killTask);
    	when(killTask.withName(anyString())).thenReturn(killTask);
    	when(killTask.withPid(anyLong())).thenReturn(killTask);   
    	
    	PowerMockito.whenNew(DisplayRunningStepTask.class).withAnyArguments().thenReturn(runningStepTask);
    	PowerMockito.whenNew(StartVideoCaptureTask.class).withAnyArguments().thenReturn(startCaptureTask);
    	PowerMockito.whenNew(StopVideoCaptureTask.class).withAnyArguments().thenReturn(stopCaptureTask);
    	
    	when(stopCaptureTask.execute()).thenReturn(stopCaptureTask);

    	PowerMockito.mockStatic(CommandTask.class);
    	PowerMockito.when(CommandTask.getInstance()).thenReturn(commandTask);
    	
        nodeServer = startServerForServlet(nodeServlet, "/" + NodeTaskServlet.class.getSimpleName() + "/*");
        serverHost = new HttpHost("localhost", ((ServerConnector)nodeServer.getConnectors()[0]).getLocalPort());
        VideoCaptureTask.resetVideoRecorders();
    }

    @AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
    	nodeServer.stop();
    }

    @Test(groups={"grid"})
    public void killProcess() throws Exception {

    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "kill")
	    	.queryString("process", "myProcess")
	    	.asString();  

    	Assert.assertEquals(response.getStatus(), 200);
    	verify(killTask).withName("myProcess");
    	verify(killTask).execute();
    }
    
    @Test(groups={"grid"})
    public void killProcessWithError() throws Exception {

    	doThrow(Exception.class).when(killTask).execute();
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "kill")
	    	.queryString("process", "myProcess")
	    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	verify(killTask).withName("myProcess");
    	verify(killTask).execute();
    }
    
    @Test(groups={"grid"})
    public void killPid() throws Exception {
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	    	.queryString("action", "killPid")
    	    	.queryString("pid", "100")
    	    	.asString(); 

    	Assert.assertEquals(response.getStatus(), 200);
    	verify(killTask).withPid(100L);
    	verify(killTask).execute();
    }

    @Test(groups={"grid"})
    public void killPidWithError() throws Exception {

    	doThrow(Exception.class).when(killTask).execute();
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "killPid")
	    	.queryString("pid", "100")
	    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	verify(killTask).withPid(100L);
    	verify(killTask).execute();
    }
    
    
    @Test(groups={"grid"})
    public void getProcessList() throws UnirestException {
    	
    	ProcessInfo p1 = new ProcessInfo();
    	p1.setPid("1000");
    	ProcessInfo p2 = new ProcessInfo();
    	p2.setPid("2000");
    	
    	PowerMockito.mockStatic(OSUtilityFactory.class);
    	when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
    	when(osUtility.getRunningProcesses("myProcess")).thenReturn(Arrays.asList(p1, p2));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
				.queryString("action", "processList")
				.queryString("name", "myProcess")
				.asString();
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "1000,2000");
    	
    }
    
    @Test(groups={"grid"})
    public void getProcessListWithError() throws UnirestException {
    	
    	ProcessInfo p1 = new ProcessInfo();
    	p1.setPid("1000");
    	ProcessInfo p2 = new ProcessInfo();
    	p2.setPid("2000");
    	
    	PowerMockito.mockStatic(OSUtilityFactory.class);
    	when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
    	when(osUtility.getRunningProcesses("myProcess")).thenThrow(new RuntimeException("error on pid"));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "processList")
    			.queryString("name", "myProcess")
    			.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("error on pid"));
    	
    }
    
    @Test(groups={"grid"})
    public void getVersion() throws UnirestException {
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "version")
    			.asString();
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	
    	JSONObject reply = new JSONObject(response.getBody());
    	Assert.assertEquals(reply.getString("version"), Utils.getCurrentversion());
    }
    
    @Test(groups={"grid"})
    public void mouseCoordinates() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	PowerMockito.when(CustomEventFiringWebDriver.getMouseCoordinates(DriverMode.LOCAL, null)).thenReturn(new Point(2, 3));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "mouseCoordinates")
    			.asString();
    	
    	Assert.assertEquals(response.getBody(), "2,3");
    	Assert.assertEquals(response.getStatus(), 200);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.getMouseCoordinates(eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void mouseCoordinatesWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "getMouseCoordinates", DriverMode.LOCAL, null);
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "mouseCoordinates")
    			.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.getMouseCoordinates(eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void leftClick() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);

    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "leftClick")
	    	.queryString("x", "0")
	    	.queryString("y", "0")
	    	.asString();
    	

    	Assert.assertEquals(response.getStatus(), 200);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(false), eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void leftClickOnMainScreen() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "leftClick")
    			.queryString("x", "0")
    			.queryString("y", "0")
    			.queryString("onlyMainScreen", "true")
    			.asString();
    	
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(true), eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void leftClickWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "leftClicOnDesktopAt", false, 0, 0, DriverMode.LOCAL, null);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "leftClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(false),eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void doubleClick() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "doubleClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(false),eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void doubleClickOnMainScreen() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "doubleClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
		.queryString("onlyMainScreen", "true")
    	.asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(true),eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }

    @Test(groups={"grid"})
    public void doubleClickWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "doubleClickOnDesktopAt", false, 0, 0, DriverMode.LOCAL, null);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "doubleClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(false),eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void rightClick() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "rightClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(false),eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void rightClickOnMainScreen() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "rightClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
		.queryString("onlyMainScreen", "true")
    	.asString();
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(true),eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }

    @Test(groups={"grid"})
    public void rightClickWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "rightClicOnDesktopAt", false, 0, 0, DriverMode.LOCAL, null);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "rightClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(false),eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void sendKeys() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "sendKeys")
	    	.queryString("keycodes", "10,20,30")
	    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.sendKeysToDesktop(eq(Arrays.asList(10, 20, 30)), eq(DriverMode.LOCAL), isNull());
    }

    @Test(groups={"grid"})
    public void sendKeysWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "sendKeysToDesktop", eq(Arrays.asList(10, 20, 30)), eq(DriverMode.LOCAL), isNull());
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
			.queryString("action", "sendKeys")
	    	.queryString("keycodes", "10,20,30")
	    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.sendKeysToDesktop(eq(Arrays.asList(10, 20, 30)), eq(DriverMode.LOCAL), isNull());
    }
    
    /**
     * Check error in command is sent back to client
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void executeCommandInError() throws UnirestException {
		doThrow(new TaskException("Error")).when(commandTask).execute();
    	
    	int status = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "command")
    	.queryString("name", "eco")
    	.queryString("arg0", "hello")
    	.asString()
    	.getStatus();
    	
    	Assert.assertEquals(status, 500);
    }
    
    @Test(groups={"grid"})
    public void executeCommand() throws UnirestException {
    	
    	when(commandTask.getResult()).thenReturn("hello guy");
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "command")
    	.queryString("name", "echo")
    	.queryString("arg0", "hello")
    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "hello guy");
    	
    	verify(commandTask).setCommand("echo", Arrays.asList("hello"), null);
    	verify(commandTask).execute();
    }
    
    /**
     * check that a long command will keep node alive above standard timeout
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void executeCommandKeepAlive() throws UnirestException {
    	
    	when(commandTask.getResult()).thenReturn("hello guy");
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "command")
    			.queryString("name", "echo")
    			.queryString("arg0", "hello")
    			.queryString("session", "1234")
    			.asString();
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "hello guy");
    	
    	verify(commandTask).setCommand("echo", Arrays.asList("hello"), null);
    	verify(commandTask).execute();
    }
    
    @Test(groups={"grid"})
    public void executeCommandWithTimeout() throws UnirestException {
    	
    	when(commandTask.getResult()).thenReturn("hello guy");
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "command")
    			.queryString("name", "echo")
    			.queryString("arg0", "hello")
    			.queryString("timeout", "40")
    			.asString();
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "hello guy");
    	
    	verify(commandTask).setCommand("echo", Arrays.asList("hello"), 40);
    	verify(commandTask).execute();
    }
    
    @Test(groups={"grid"})
    public void writeText() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "writeText")
    	.queryString("text", "foobar")
    	.asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.writeToDesktop(eq("foobar"), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void writeTextWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "writeToDesktop", eq("foobar"), eq(DriverMode.LOCAL), isNull());
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "writeText")
    			.queryString("text", "foobar")
    			.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.writeToDesktop(eq("foobar"), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void displayRunningStep() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
 
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "displayRunningStep")
    			.queryString("stepName", "foobar")
    			.queryString("session", "1234")
    			.asString();
    	
    	verify(runningStepTask).execute();
    	
    	Assert.assertEquals(response.getStatus(), 200);
    }
    
    @Test(groups={"grid"})
    public void displayRunningStepWithError() throws Exception {
    	
    	when(runningStepTask.execute()).thenThrow(new WebDriverException("driver error"));
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "displayRunningStep")
    			.queryString("stepName", "foobar")
    			.queryString("session", "1234")
    			.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    }
    
    @Test(groups={"grid"})
    public void uploadFile() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "uploadFile")
    	.queryString("name", "foobar.txt")
    	.field("content", "someText")
    	.asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq("someText"), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void uploadFileWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "uploadFile", eq("foobar.txt"), eq("someText"), eq(DriverMode.LOCAL), isNull());
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "uploadFile")
    	.queryString("name", "foobar.txt")
    	.field("content", "someText")
    	.asString();

    	Assert.assertEquals(response.getStatus(), 500);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq("someText"), eq(DriverMode.LOCAL), isNull());
    }

    /**
     * New way of uploading file, through body
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void uploadFile2() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	String b64png = Base64.getEncoder().encodeToString(IOUtils.toByteArray(TestNodeTaskServlet.class.getClassLoader().getResourceAsStream("upload.png")));
    	byte[] png = IOUtils.resourceToByteArray("upload.png", TestNodeTaskServlet.class.getClassLoader());
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString())
		    	.queryString("action", "uploadFile")
		    	.queryString("name", "foobar.txt")
		    	.body(png)
		    	.asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq(b64png), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void captureDesktop() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull())).thenReturn("ABCDEF");
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "screenshot")
	    	.asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull());
    	Assert.assertEquals(response.getBody(), "ABCDEF");
    }
    
    @Test(groups={"grid"})
    public void captureDesktopWithError() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull())).thenThrow(new WebDriverException("capture"));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "screenshot")
    			.asString();

    	Assert.assertEquals(response.getStatus(), 500);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull());
    	Assert.assertTrue(response.getBody().contains("capture"));
    }
    
    @Test(groups={"grid"})
    public void startCapture() throws Exception {
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
			.queryString("action", "startVideoCapture")
			.queryString("session", "1234567890-4").asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	verify(startCaptureTask).execute();
    }
    
    @Test(groups={"grid"})
    public void startCaptureWithError() throws Exception {
    	when(startCaptureTask.execute()).thenThrow(new WebDriverException("recorder"));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "startVideoCapture")
    	.queryString("session", "1234567890-4").asString();

    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("recorder"));
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

		File videoFile = File.createTempFile("video-", ".avi");
		videoFile.delete();
		
		// 
    	when(stopCaptureTask.getVideoFile()).thenReturn(tempVideo);

    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "stopVideoCapture")
	    	.queryString("session", "1234567890-2").asFile(videoFile.getAbsolutePath());

    	Assert.assertEquals(FileUtils.readFileToString(videoFile, StandardCharsets.UTF_8), "foo");
    	
    	// check video file has not been deleted
    	Assert.assertTrue(tempVideo.exists());
    }
    
    /**
     * Start and stop capture. Check file is written
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCaptureWithError() throws UnirestException, IOException {

    	when(stopCaptureTask.getVideoFile()).thenThrow(new SeleniumGridException("error stopping"));
    	
    	File tempVideo = File.createTempFile("video-", ".avi");
    	FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);

    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "1234567890-2").asString();
    	

    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("stop"));
    	Assert.assertNull(VideoCaptureTask.getVideoRecorders().get("1234567890-2"));
    	
    }
    
    /**
     * Check when no file has been captured. Data returned is empty
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCaptureNoFile() throws UnirestException, IOException {

    	when(stopCaptureTask.getVideoFile()).thenReturn(null);
    	File videoFile = File.createTempFile("video-", ".avi");

    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "12345678901").asFile(videoFile.getAbsolutePath());
    	
    	
    	Assert.assertNull(VideoCaptureTask.getVideoRecorders().get("12345678901"));
    	Assert.assertEquals(FileUtils.readFileToString(videoFile, StandardCharsets.UTF_8), "");
    }
    
    @Test(groups={"grid"})
    public void stopCaptureWithoutStart() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	File videoFile = File.createTempFile("video-", ".avi");
    	
    	File tempVideo = File.createTempFile("video-", ".avi");
    	FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);
    	when(CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenReturn(tempVideo);
    	
    	HttpResponse<File> videoResponse = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "12345678902").asFile(videoFile.getAbsolutePath());
    	
    	
    	Assert.assertNull(VideoCaptureTask.getVideoRecorders().get("12345678902"));
    	Assert.assertEquals(FileUtils.readFileToString(videoFile, StandardCharsets.UTF_8), "");
    }
    
    @Test(groups={"grid"})
    public void driverPids() throws Exception {

    	BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));
    	
        PowerMockito.mockStatic(OSUtility.class);
		when(OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);
		
        Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
        browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
        browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));
        
        when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
        doReturn(Arrays.asList(200L)).when(bi1).getDriverAndBrowserPid(anyList());
        
        HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "driverPids")
    			.queryString("existingPids", "100,300")
    			.queryString("browserName", "chrome")
    			.queryString("browserVersion", "85.0")
    			.asString();
        

    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "200");
    }
    
    @Test(groups={"grid"})
    public void driverPidsWithError() throws Exception {
    	
    	BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));
    	
    	PowerMockito.mockStatic(OSUtility.class);
    	when(OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);
    	
    	Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
    	browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
    	browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));
    	
    	when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
    	doThrow(new RuntimeException("pids")).when(bi1).getDriverAndBrowserPid(anyList());
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "driverPids")
    			.queryString("existingPids", "100,300")
    			.queryString("browserName", "chrome")
    			.queryString("browserVersion", "85.0")
    			.asString();
    	
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("pids"));
    }
    
    @Test(groups={"grid"})
    public void browserAndDriverPids() throws Exception {
    	
    	BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));
    	
    	PowerMockito.mockStatic(OSUtility.class);
    	when(OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);
    	
    	Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
    	browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
    	browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));
    	
    	when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
    	doReturn(Arrays.asList(200L, 400L)).when(bi1).getAllBrowserSubprocessPids(Arrays.asList(100L));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "browserAndDriverPids")
    			.queryString("parentPids", "100")
    			.queryString("browserName", "chrome")
    			.queryString("browserVersion", "85.0")
    			.asString();
    	
    	
    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "200,400");
    }
    

    @Test(groups={"grid"})
    public void browserAndDriverPidsWithError() throws Exception {
    	
    	BrowserInfo bi1 = spy(new BrowserInfo(BrowserType.CHROME, "85.0", null));
    	
    	PowerMockito.mockStatic(OSUtility.class);
    	when(OSUtility.getCurrentPlatorm()).thenReturn(Platform.WINDOWS);
    	
    	Map<BrowserType, List<BrowserInfo>> browsers = new HashMap<>();
    	browsers.put(BrowserType.CHROME, Arrays.asList(bi1));
    	browsers.put(BrowserType.FIREFOX, Arrays.asList(spy(new BrowserInfo(BrowserType.FIREFOX, "75.0", null))));
    	
    	when(OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browsers);
    	doThrow(new RuntimeException("pids")).when(bi1).getAllBrowserSubprocessPids(Arrays.asList(100L));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "browserAndDriverPids")
    			.queryString("parentPids", "100")
    			.queryString("browserName", "chrome")
    			.queryString("browserVersion", "85.0")
    			.asString();

    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("pids"));
    }

    /**
     * test we kill browsers and drivers when devMode is disabled
     * @throws UnirestException
     * @throws IOException 
     */
    @Test(groups={"grid"})
    public void cleanNode() throws UnirestException, IOException {
    	PowerMockito.mockStatic(OSUtilityFactory.class);
    	when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
    	
    	PowerMockito.mockStatic(LaunchConfig.class);
    	when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    	when(launchConfig.getDevMode()).thenReturn(false);
    	
    	PowerMockito.mockStatic(FileUtils.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "clean")
    	.asString();
    	
    	verify(osUtility).killAllWebBrowserProcess(true);
    	verify(osUtility).killAllWebDriverProcess();
    	PowerMockito.verifyStatic(FileUtils.class);
    	FileUtils.cleanDirectory(any(File.class));
    	
    }
    
    @Test(groups={"grid"})
    public void cleanNodeResetWindowsProxy() throws UnirestException, IOException {
    	PowerMockito.mockStatic(OSUtilityFactory.class);
    	when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
    	
    	PowerMockito.mockStatic(LaunchConfig.class);
    	when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    	when(launchConfig.getDevMode()).thenReturn(false);
    	when(launchConfig.getProxyConfig()).thenReturn(proxy);
    	
    	PowerMockito.mockStatic(FileUtils.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "clean")
    	.asString();
    	
    	// check proxy is reset
    	verify(osUtility).setSystemProxy(proxy);
    	
    }
    
    @Test(groups={"grid"})
    public void cleanNodeDoNotResetWindowsProxy2() throws UnirestException, IOException {
    	PowerMockito.mockStatic(OSUtilityFactory.class);
    	when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
    	
    	PowerMockito.mockStatic(LaunchConfig.class);
    	when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    	when(launchConfig.getDevMode()).thenReturn(false);
    	when(launchConfig.getProxyConfig()).thenReturn(null);
    	
    	PowerMockito.mockStatic(FileUtils.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "clean")
    	.asString();
    	
    	// check proxy is reset
    	verify(osUtility, never()).setSystemProxy(proxy);
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
    	
    	try {
	    	PowerMockito.mockStatic(OSUtilityFactory.class);
	    	when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
	    	
	    	PowerMockito.mockStatic(LaunchConfig.class);
	    	when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
	    	when(launchConfig.getDevMode()).thenReturn(false);
	    	
	    	PowerMockito.mockStatic(FileUtils.class);
	    	
	    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "clean")
	    	.asString();
	    	
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
    	
    	try {
    		PowerMockito.mockStatic(OSUtilityFactory.class);
    		when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
    		
    		PowerMockito.mockStatic(LaunchConfig.class);
    		when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    		when(launchConfig.getDevMode()).thenReturn(false);
    		
    		PowerMockito.mockStatic(FileUtils.class);
    		
    		Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    		.queryString("action", "clean")
    		.asString();
    		
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
    	PowerMockito.mockStatic(OSUtilityFactory.class);
    	when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
    	
    	PowerMockito.mockStatic(LaunchConfig.class);
    	when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    	when(launchConfig.getDevMode()).thenReturn(true);
    	
    	PowerMockito.mockStatic(FileUtils.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "clean")
    	.asString();
    	
    	verify(osUtility, never()).killAllWebBrowserProcess(true);
    	verify(osUtility, never()).killAllWebDriverProcess();
    	PowerMockito.verifyStatic(FileUtils.class, never());
    	FileUtils.cleanDirectory(any(File.class));
    	
    }
    
 
}
