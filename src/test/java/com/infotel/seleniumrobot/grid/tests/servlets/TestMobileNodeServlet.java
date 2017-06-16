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

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openqa.grid.common.exception.CapabilityNotPresentOnTheGridException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.MobileNodeServlet;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.mobile.MobileDeviceSelector;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.DriverMode;


public class TestMobileNodeServlet extends BaseServletTest {

    private Server mobileInfoServer;
    private HttpHost serverHost;
    
    @Mock
    MobileDeviceSelector mobileDeviceSelector;
    
    @InjectMocks
    MobileNodeServlet nodeServlet = new MobileNodeServlet();

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
        mobileInfoServer = startServerForServlet(nodeServlet, "/" + MobileNodeServlet.class.getSimpleName() + "/*");
        serverHost = new HttpHost("localhost", ((ServerConnector)mobileInfoServer.getConnectors()[0]).getLocalPort());
    }

    @AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
    	mobileInfoServer.stop();
    }
    
    /**
     * When using "platform" key, DesiredCapabilities maps it to "Platform" object which throws an exception when 
     * creating a JSONObject from this map
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void getNoErrorWithPlatform() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	DesiredCapabilities caps = new DesiredCapabilities();
    	caps.setCapability("platform", "ANDROID");
    	DesiredCapabilities updatedCaps = new DesiredCapabilities();
    	updatedCaps.setCapability("platform", "ANDROID");
    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(caps, DriverMode.LOCAL)).thenReturn(updatedCaps);
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	for (Entry<String, ?> entry: caps.asMap().entrySet()) {
    		builder.setParameter(entry.getKey(), entry.getValue().toString());
    	}
    	
    	HttpGet httpGet = new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	JSONObject reply = new JSONObject(IOUtils.toString(execute.getEntity().getContent(), Charset.forName("UTF-8")));
    	
    	Assert.assertEquals(reply.getString("platform"), "ANDROID");
    	
    }

    @Test(groups={"grid"})
    public void getShouldReturnUpdatedCaps() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();

    	DesiredCapabilities caps = new DesiredCapabilities();
    	caps.setCapability("platformName", "android");
    	caps.setCapability("platformVersion", "5.0");
    	caps.setCapability("deviceName", "Nexus 5");
    	DesiredCapabilities updatedCaps = new DesiredCapabilities();
    	updatedCaps.setCapability("platformName", "android");
    	updatedCaps.setCapability("platformVersion", "5.0");
    	updatedCaps.setCapability("deviceName", "145687");
    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(caps, DriverMode.LOCAL)).thenReturn(updatedCaps);
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	for (Entry<String, ?> entry: caps.asMap().entrySet()) {
    		builder.setParameter(entry.getKey(), entry.getValue().toString());
    	}

    	HttpGet httpGet = new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	JSONObject reply = new JSONObject(IOUtils.toString(execute.getEntity().getContent(), Charset.forName("UTF-8")));
    	
    	Assert.assertEquals(reply.getString("deviceName"), "145687");
   
    }
    
    @Test(groups={"grid"})
    public void getShouldAddChromeDriverExecutable() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	DesiredCapabilities caps = new DesiredCapabilities();
    	caps.setCapability("platformName", "android");
    	DesiredCapabilities updatedCaps = new DesiredCapabilities();
    	updatedCaps.setCapability("platformName", "android");
    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(caps, DriverMode.LOCAL)).thenReturn(updatedCaps);
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	for (Entry<String, ?> entry: caps.asMap().entrySet()) {
    		builder.setParameter(entry.getKey(), entry.getValue().toString());
    	}
    	
    	HttpGet httpGet = new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	JSONObject reply = new JSONObject(IOUtils.toString(execute.getEntity().getContent(), Charset.forName("UTF-8")));
    	
    	Assert.assertEquals(reply.getString("platformName"), "android");
    	Assert.assertTrue(reply.getString("chromedriverExecutable").contains(Utils.getDriverDir().toString()));
    	
    }
    
    @Test(groups={"grid"})
    public void getShouldThrowError() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();

    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(Mockito.any(DesiredCapabilities.class), Matchers.eq(DriverMode.LOCAL))).thenThrow(new ConfigurationException("device not found"));
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	
    	HttpGet httpGet = new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 404);
    	
    }
    
    @Test(groups={"grid"}, expectedExceptions=CapabilityNotPresentOnTheGridException.class)
    public void testServletClientOnSuccess() throws IOException, URISyntaxException {
    	MobileNodeServletClient client = new MobileNodeServletClient("localhost", ((ServerConnector)mobileInfoServer.getConnectors()[0]).getLocalPort());
    	
    	DesiredCapabilities caps = new DesiredCapabilities();
    	caps.setCapability("platformName", "android");
    	caps.setCapability("platformVersion", "5.0");
    	caps.setCapability("deviceName", "Nexus 5");
    	
    	DesiredCapabilities updatedCaps = new DesiredCapabilities();
    	updatedCaps.setCapability("platformName", "android");
    	updatedCaps.setCapability("platformVersion", "5.0");
    	updatedCaps.setCapability("deviceName", "145687");
    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(caps, DriverMode.LOCAL)).thenReturn(updatedCaps);
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	
    	DesiredCapabilities newCaps = client.updateCapabilities(caps);
    	Assert.assertEquals(newCaps.getCapability("deviceName"), "145687");
    }
    
    @Test(groups={"grid"}, expectedExceptions=CapabilityNotPresentOnTheGridException.class)
    public void testServletClientOnError() throws IOException, URISyntaxException {
    	MobileNodeServletClient client = new MobileNodeServletClient("localhost", ((ServerConnector)mobileInfoServer.getConnectors()[0]).getLocalPort());
    	
    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(Mockito.any(DesiredCapabilities.class), Matchers.eq(DriverMode.LOCAL))).thenThrow(new ConfigurationException("device not found"));
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	
    	DesiredCapabilities caps = new DesiredCapabilities();
    	caps.setCapability("platformName", "android");
    	caps.setCapability("platformVersion", "5.0");
    	caps.setCapability("deviceName", "Nexus 5");
    	client.updateCapabilities(caps);
    }

}
