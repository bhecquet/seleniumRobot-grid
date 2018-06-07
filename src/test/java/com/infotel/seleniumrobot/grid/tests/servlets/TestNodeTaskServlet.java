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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.NodeRestartTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.driver.screenshots.VideoRecorder;

@PrepareForTest(CustomEventFiringWebDriver.class)
@PowerMockIgnore("javax.net.ssl.*") // to avoid error java.security.NoSuchAlgorithmException: class configured for SSLContext: sun.security.ssl.SSLContextImpl$TLS10Context not a SSLContext
public class TestNodeTaskServlet extends BaseServletTest {

    private Server nodeServer;
    private HttpHost serverHost;
    
    @Mock
    NodeRestartTask restartTask;
    
    @Mock
    KillTask killTask;
    
    @Mock
    VideoRecorder recorder;
    
    @InjectMocks
    NodeTaskServlet nodeServlet = new NodeTaskServlet();

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
        nodeServer = startServerForServlet(nodeServlet, "/" + NodeTaskServlet.class.getSimpleName() + "/*");
        serverHost = new HttpHost("localhost", ((ServerConnector)nodeServer.getConnectors()[0]).getLocalPort());
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
    public void killProcess() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/NodeTaskServlet/");
    	builder.setParameter("action", "kill");
    	builder.setParameter("process", "myProcess");
    	
    	HttpPost httpPost = new HttpPost(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpPost);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 200);   
    	
    	verify(killTask).execute();
    }
    
    @Test(groups={"grid"})
    public void getVersion() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/NodeTaskServlet/");
    	builder.setParameter("action", "version");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 200);
    	
    	JSONObject reply = new JSONObject(IOUtils.toString(execute.getEntity().getContent(), Charset.forName("UTF-8")));
    	Assert.assertEquals(reply.getString("version"), Utils.getCurrentversion());
    }
    
    @Test(groups={"grid"})
    public void leftClick() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);

    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "leftClic")
	    	.queryString("x", "0")
	    	.queryString("y", "0")
	    	.asString();
    	
    	PowerMockito.verifyStatic();
    	CustomEventFiringWebDriver.leftClicOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void rightClick() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "rightClic")
    	.queryString("x", "0")
    	.queryString("y", "0")
    	.asString();
    	
    	PowerMockito.verifyStatic();
    	CustomEventFiringWebDriver.rightClicOnDesktopAt(eq(0), eq(0), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void sendKeys() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "sendKeys")
    	.queryString("keycodes", "10,20,30")
    	.asString();
    	
    	PowerMockito.verifyStatic();
    	CustomEventFiringWebDriver.sendKeysToDesktop(eq(Arrays.asList(10, 20, 30)), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void writeText() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "writeText")
    	.queryString("text", "foobar")
    	.asString();
    	
    	PowerMockito.verifyStatic();
    	CustomEventFiringWebDriver.writeToDesktop(eq("foobar"), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void uploadFile() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	Unirest.post(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "uploadFile")
    	.queryString("name", "foobar.txt")
    	.queryString("content", "someText")
    	.asString();
    	
    	PowerMockito.verifyStatic();
    	CustomEventFiringWebDriver.uploadFile(eq("foobar.txt"), eq("someText"), eq(DriverMode.LOCAL), isNull());
    }
    
    @Test(groups={"grid"})
    public void captureDesktop() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull())).thenReturn("ABCDEF");
    	
    	String data = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "screenshot")
	    	.asString().getBody();
    	
    	PowerMockito.verifyStatic();
    	CustomEventFiringWebDriver.captureDesktopToBase64String(eq(DriverMode.LOCAL), isNull());
    	Assert.assertEquals(data, "ABCDEF");
    }
    
    @Test(groups={"grid"})
    public void startCapture() throws UnirestException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	when(CustomEventFiringWebDriver.startVideoCapture(eq(DriverMode.LOCAL), isNull(), any(File.class), anyString())).thenReturn(recorder);
    	
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
			.queryString("action", "startVideoCapture")
			.queryString("session", "1234567890").asString();
    	
    	Assert.assertEquals(NodeTaskServlet.getVideoRecorders().get("1234567890"), recorder);
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
    	FileUtils.write(tempVideo, "foo", Charset.forName("UTF-8"));
    	when(CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenReturn(tempVideo);
    	
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "startVideoCapture")
	    	.queryString("session", "1234567890").asString();
    	HttpResponse<InputStream> videoResponse = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
	    	.queryString("action", "stopVideoCapture")
	    	.queryString("session", "1234567890").asBinary();
    	
    	InputStream videoI = videoResponse.getBody();
		
		File videoFile = File.createTempFile("video-", ".avi");
		FileOutputStream os = new FileOutputStream(videoFile);
		IOUtils.copy(videoI, os);
		os.close();
    	
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("1234567890"));
    	Assert.assertEquals(FileUtils.readFileToString(videoFile, Charset.forName("UTF-8")), "foo");
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
    	
    	Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    	.queryString("action", "startVideoCapture")
    	.queryString("session", "1234567890").asString();
    	HttpResponse<InputStream> videoResponse = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "1234567890").asBinary();
    	
    	InputStream videoI = videoResponse.getBody();
    	
    	File videoFile = File.createTempFile("video-", ".avi");
    	FileOutputStream os = new FileOutputStream(videoFile);
    	IOUtils.copy(videoI, os);
    	os.close();
    	
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("1234567890"));
    	Assert.assertEquals(FileUtils.readFileToString(videoFile, Charset.forName("UTF-8")), "");
    }
    
    @Test(groups={"grid"})
    public void stopCaptureWithoutStart() throws UnirestException, IOException {
    	PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
    	
    	File tempVideo = File.createTempFile("video-", ".avi");
    	FileUtils.write(tempVideo, "foo", Charset.forName("UTF-8"));
    	when(CustomEventFiringWebDriver.stopVideoCapture(eq(DriverMode.LOCAL), isNull(), any(VideoRecorder.class))).thenReturn(tempVideo);
    	
    	HttpResponse<InputStream> videoResponse = Unirest.get(String.format("%s%s", serverHost.toURI().toString(), "/NodeTaskServlet/"))
    			.queryString("action", "stopVideoCapture")
    			.queryString("session", "1234567890").asBinary();
    	
    	InputStream videoI = videoResponse.getBody();
    	
    	File videoFile = File.createTempFile("video-", ".avi");
    	FileOutputStream os = new FileOutputStream(videoFile);
    	IOUtils.copy(videoI, os);
    	os.close();
    	
    	Assert.assertNull(NodeTaskServlet.getVideoRecorders().get("1234567890"));
    	Assert.assertEquals(FileUtils.readFileToString(videoFile, Charset.forName("UTF-8")), "");
    }
    
 
}
