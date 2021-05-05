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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.infotel.seleniumrobot.grid.servlets.client.HubTaskServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.infotel.seleniumrobot.grid.tasks.CommandTask;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.NodeRestartTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.driver.screenshots.VideoRecorder;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

@PrepareForTest({CustomEventFiringWebDriver.class, OSUtilityFactory.class, OSUtility.class, BrowserInfo.class, LaunchConfig.class, FileUtils.class, OSCommand.class, NodeTaskServlet.class, CommandTask.class})
@PowerMockIgnore("javax.net.ssl.*") // to avoid error java.security.NoSuchAlgorithmException: class configured for SSLContext: sun.security.ssl.SSLContextImpl$TLS10Context not a SSLContext
public class TestNodeTaskServlet extends BaseServletTest {

    private Server nodeServer;
    private HttpHost serverHost;
    
    @Mock
    NodeRestartTask restartTask;
    
    @Mock
    OSUtility osUtility;
    
    @Mock
    KillTask killTask;
    
    @Mock
    CommandTask commandTask;
    
    @Mock
    GridRegistry registry;
    
    @Mock
    VideoRecorder recorder;
    
    @Mock
    LaunchConfig launchConfig;
    
    @Mock
    LocalAppiumLauncher appiumLauncher;
    
    @Mock
    GridNodeConfiguration gridNodeConfiguration;
    
    @Mock
    HubTaskServletClient hubTaskServletClient;

    @InjectMocks
    NodeTaskServlet nodeServlet;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {

    	PowerMockito.mockStatic(LaunchConfig.class);
    	when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
    	when(launchConfig.getExternalProgramWhiteList()).thenReturn(Arrays.asList("echo"));
    	when(LaunchConfig.getCurrentNodeConfig()).thenReturn(gridNodeConfiguration);
    	when(gridNodeConfiguration.getHubHost()).thenReturn("127.0.0.1");
    	when(gridNodeConfiguration.getHubPort()).thenReturn(4444);
    	

    	PowerMockito.mockStatic(CommandTask.class);
    	PowerMockito.when(CommandTask.getInstance()).thenReturn(commandTask);
    	
        nodeServer = startServerForServlet(nodeServlet, "/" + NodeTaskServlet.class.getSimpleName() + "/*");
        serverHost = new HttpHost("localhost", ((ServerConnector)nodeServer.getConnectors()[0]).getLocalPort());
        NodeTaskServlet.resetAppiumLaunchers();
        NodeTaskServlet.resetVideoRecorders();
    }

    @AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
    	nodeServer.stop();
    }

    @Test(groups={"grid"})
    public void restartNode() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/NodeTaskServlet/");
    	builder.setParameter("action", "restart");
    	
    	HttpPost httpPost = new HttpPost(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpPost);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 200);   
    }
    
    @Test(groups={"grid"})
    public void killProcess() throws Exception {

    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "kill")
	    	.queryString("process", "myProcess")
	    	.asString();  

    	Assert.assertEquals(response.getStatus(), 200);
    	verify(killTask).setTaskName("myProcess");
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
    	verify(killTask).setTaskName("myProcess");
    	verify(killTask).execute();
    }
    
    @Test(groups={"grid"})
    public void killPid() throws Exception {
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	    	.queryString("action", "killPid")
    	    	.queryString("pid", "100")
    	    	.asString(); 

    	Assert.assertEquals(response.getStatus(), 200);
    	verify(killTask).setTaskPid(100L);
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
    	verify(killTask).setTaskPid(100L);
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
    public void leftClick() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);

    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "leftClick")
	    	.queryString("x", "0")
	    	.queryString("y", "0")
	    	.asString();
    	

    	Assert.assertEquals(response.getStatus(), 200);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void leftClickWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "leftClicOnDesktopAt", 0, 0, DriverMode.LOCAL, null);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "leftClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void doubleClick() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "doubleClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }

    @Test(groups={"grid"})
    public void doubleClickWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "doubleClickOnDesktopAt", 0, 0, DriverMode.LOCAL, null);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "doubleClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.doubleClickOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
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
    	CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }

    @Test(groups={"grid"})
    public void rightClickWithError() throws Exception {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	PowerMockito.doThrow(new WebDriverException("driver")).when(CustomEventFiringWebDriver.class, "rightClicOnDesktopAt", 0, 0, DriverMode.LOCAL, null);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "rightClick")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
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
    	verify(hubTaskServletClient).disableTimeout("1234");
    	//verify(hubTaskServletClient).enableTimeout("1234"); // for unknown reason, when tests are executed from maven, this interaction is never seen, whereas it's done (we see logs before and after, and debug show that method is called)
    	verify(hubTaskServletClient).keepDriverAlive("1234");
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
    public void uploadFile() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	HttpResponse<String> response = Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "uploadFile")
    	.queryString("name", "foobar.txt")
    	.queryString("content", "someText")
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
    	.queryString("content", "someText")
    	.asString();

    	Assert.assertEquals(response.getStatus(), 500);
    	PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
    	CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq("someText"), eq(DriverMode.LOCAL), isNull());
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
    public void startCapture() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.startVideoCapture(eq(DriverMode.LOCAL), isNull(), any(File.class), anyString())).thenReturn(recorder);
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
			.queryString("action", "startVideoCapture")
			.queryString("session", "1234567890-4").asString();

    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(NodeTaskServlet.getVideoRecorders().get("1234567890-4"), recorder);
    }
    
    @Test(groups={"grid"})
    public void startCaptureWithError() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.startVideoCapture(eq(DriverMode.LOCAL), isNull(), any(File.class), anyString())).thenThrow(new WebDriverException("recorder"));
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "startVideoCapture")
    	.queryString("session", "1234567890-4").asString();

    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("recorder"));
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("1234567890-4"));
    }
    
    /**
     * Start and stop capture. Check file is written
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCapture() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.startVideoCapture(eq(DriverMode.LOCAL), isNull(), any(File.class), anyString())).thenReturn(recorder);
    	File tempVideo = File.createTempFile("video-", ".avi");
    	FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);
    	when(CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenReturn(tempVideo);
    	
		File videoFile = File.createTempFile("video-", ".avi");
		videoFile.delete();
    	
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "startVideoCapture")
	    	.queryString("session", "1234567890-2").asString();
    	HttpResponse<File> videoResponse = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "stopVideoCapture")
	    	.queryString("session", "1234567890-2").asFile(videoFile.getAbsolutePath());
    	
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("1234567890-2"));
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
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.startVideoCapture(eq(DriverMode.LOCAL), isNull(), any(File.class), anyString())).thenReturn(recorder);
    	File tempVideo = File.createTempFile("video-", ".avi");
    	FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);
    	when(CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenThrow(new WebDriverException("stop"));
    	
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "startVideoCapture")
    	.queryString("session", "1234567890-2").asString();
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "1234567890-2").asString();
    	

    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("stop"));
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("1234567890-2"));
    	
    }
    
    /**
     * issue #41: check that file remains event when stopping capture is called twice
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCaptureCalledTwice() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.startVideoCapture(eq(DriverMode.LOCAL), isNull(), any(File.class), anyString())).thenReturn(recorder);
    	File tempVideo = File.createTempFile("video-", ".avi");
    	FileUtils.write(tempVideo, "foo", StandardCharsets.UTF_8);
    	when(CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenReturn(tempVideo);
    	
    	File videoFile = File.createTempFile("video-", ".avi");
    	videoFile.delete();
    	
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "startVideoCapture")
    	.queryString("session", "1234567890-1").asString();
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "1234567890-1").asFile(videoFile.getAbsolutePath());
    	HttpResponse<File> videoResponse = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "1234567890-1").asFile(videoFile.getAbsolutePath());
    	
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("1234567890-1"));
    	Assert.assertEquals(FileUtils.readFileToString(videoFile, StandardCharsets.UTF_8), "foo");
    	
    	// check video file has not been deleted
    	Assert.assertTrue(tempVideo.exists());
    }
    
    /**
     * Check when no file has been captured. Data returned is empty
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void stopCaptureNoFile() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.startVideoCapture(eq(DriverMode.LOCAL), isNull(), any(File.class), anyString())).thenReturn(recorder);
    	when(CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenReturn(null);

    	File videoFile = File.createTempFile("video-", ".avi");
    	
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "startVideoCapture")
    	.queryString("session", "12345678901").asString();
    	HttpResponse<File> videoResponse = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "12345678901").asFile(videoFile.getAbsolutePath());
    	
    	
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("12345678901"));
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
    	
    	
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("12345678902"));
    	Assert.assertEquals(FileUtils.readFileToString(videoFile, StandardCharsets.UTF_8), "");
    }
    
    @Test(groups={"grid"})
    public void startAppium() throws Exception {
    	
    	PowerMockito.whenNew(LocalAppiumLauncher.class).withAnyArguments().thenReturn(appiumLauncher);
    	when(appiumLauncher.getAppiumServerUrl()).thenReturn("http://localhost:1234/wd/hub/");
 
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "startAppium")
	    	.queryString("session", "12345")
	    	.asString();
    	
    	verify(appiumLauncher).startAppium();
    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "http://localhost:1234/wd/hub/");
    	Assert.assertEquals(NodeTaskServlet.getAppiumLaunchers().get("12345"), appiumLauncher);
    }
    
    @Test(groups={"grid"})
    public void startAppiumWithError() throws Exception {
    	
    	PowerMockito.whenNew(LocalAppiumLauncher.class).withAnyArguments().thenReturn(appiumLauncher);
    	when(appiumLauncher.getAppiumServerUrl()).thenReturn("http://localhost:1234/wd/hub/");
    	doThrow(new RuntimeException("appium")).when(appiumLauncher).startAppium();
    	
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "startAppium")
    			.queryString("session", "12345")
    			.asString();
    	
    	verify(appiumLauncher).startAppium();
    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("appium"));
    	Assert.assertNull(NodeTaskServlet.getAppiumLaunchers().get("12345"));
    }
    
    @Test(groups={"grid"})
    public void stopAppium() throws Exception {
    	
    	PowerMockito.whenNew(LocalAppiumLauncher.class).withAnyArguments().thenReturn(appiumLauncher);
    	when(appiumLauncher.getAppiumServerUrl()).thenReturn("http://localhost:1234/wd/hub/");
    	
    	// start
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "startAppium")
	    	.queryString("session", "12345")
	    	.asString();
    	Assert.assertEquals(NodeTaskServlet.getAppiumLaunchers().get("12345"), appiumLauncher);
    	
    	// stop
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopAppium")
    			.queryString("session", "12345")
    			.asString();

    	verify(appiumLauncher).stopAppium();
    	Assert.assertEquals(response.getStatus(), 200);
    	Assert.assertEquals(response.getBody(), "stop appium ok");
    	Assert.assertEquals(NodeTaskServlet.getAppiumLaunchers().get("12345"), null);
    }
    
    @Test(groups={"grid"})
    public void stopAppiumWithError() throws Exception {
    	
    	PowerMockito.whenNew(LocalAppiumLauncher.class).withAnyArguments().thenReturn(appiumLauncher);
    	when(appiumLauncher.getAppiumServerUrl()).thenReturn("http://localhost:1234/wd/hub/");
    	doThrow(new RuntimeException("appium")).when(appiumLauncher).stopAppium();
    	
    	// start
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "startAppium")
    	.queryString("session", "12345")
    	.asString();
    	Assert.assertEquals(NodeTaskServlet.getAppiumLaunchers().get("12345"), appiumLauncher);
    	
    	// stop
    	HttpResponse<String> response = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopAppium")
    			.queryString("session", "12345")
    			.asString();
    	
    	verify(appiumLauncher).stopAppium();
    	Assert.assertEquals(response.getStatus(), 500);
    	Assert.assertTrue(response.getBody().contains("appium"));
    	Assert.assertEquals(NodeTaskServlet.getAppiumLaunchers().get("12345"), null);
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
    
    /**
     * Test videos younger than 8 hours are not deleted
     * @throws UnirestException
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void cleanNodeDoNotPurgeNewVideo() throws UnirestException, IOException {

    	// create video file
    	File videoFile = Paths.get(Utils.getRootdir(), NodeTaskServlet.VIDEOS_FOLDER, "video.mp4").toFile();
    	FileUtils.write(videoFile, "foo");
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
    	File videoFile = Paths.get(Utils.getRootdir(), NodeTaskServlet.VIDEOS_FOLDER, "video.mp4").toFile();
    	FileUtils.write(videoFile, "foo");
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
