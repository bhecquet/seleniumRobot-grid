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
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.node.config.NodeOptions;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.server.NodeStatusServlet;
import com.infotel.seleniumrobot.grid.utils.GridStatus;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

@PrepareForTest({LaunchConfig.class})
@PowerMockIgnore({"javax.net.ssl.*", // to avoid error java.security.NoSuchAlgorithmException: class configured for SSLContext: sun.security.ssl.SSLContextImpl$TLS10Context not a SSLContext
				"javax.management.*"}) // to avoid error: java.lang.LinkageError: loader constraint violation: loader (instance of org/powermock/core/classloader/MockClassLoader) previously initiated loading for a different type with name "javax/management/MBeanServer"
public class TestNodeStatusServlet extends BaseServletTest {

    private Server nodeServer;
    private String url;

    @Mock
    NodeOptions nodeOptions;
    
    @Mock
    BaseServerOptions serverOptions;
    
    GridNodeConfiguration gridNodeConfiguration;
    
    @InjectMocks
    NodeStatusServlet nodeServlet = new NodeStatusServlet();
    

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
        nodeServer = startServerForServlet(nodeServlet, "/" + NodeStatusServlet.class.getSimpleName() + "/*");
        url = String.format("http://localhost:%d/NodeStatusServlet/", ((ServerConnector)nodeServer.getConnectors()[0]).getLocalPort());
        
        PowerMockito.mockStatic(LaunchConfig.class);
        
        gridNodeConfiguration = spy(new GridNodeConfiguration());
        
        LaunchConfig launchConfig = new LaunchConfig(new String[] {"node"});
        when(LaunchConfig.getCurrentNodeConfig()).thenReturn(gridNodeConfiguration);
        when(LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
        when(gridNodeConfiguration.getNodeOptions()).thenReturn(nodeOptions);
        when(gridNodeConfiguration.getServerOptions()).thenReturn(serverOptions);
        when(nodeOptions.getPublicGridUri()).thenReturn(Optional.of(new URI("http://localhost:4444")));
        
        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("browserName", "chrome");
        caps.setCapability("customCap", "capValue");
        when(gridNodeConfiguration.getCapabilities()).thenReturn(Arrays.asList(caps));
    }

    @AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
    	nodeServer.stop();
    }

    /**
     * Check that with no format defined, HTML is returned
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void getHtmlFormat() throws IOException, URISyntaxException, UnirestException {
    	String body = Unirest.get(url).asString().getBody();
    	
    	Assert.assertTrue(body.contains("<html>"));
    	Assert.assertTrue(body.contains("<div id=\"freeMemory\"> Free Memory: "));
    	Assert.assertTrue(body.contains("<h1 class=\"iframeTitre\" >System informations</h1>"));
    }
    
    /**
     * Check that with JSON format defined, json is returned
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void getJsonFormat() throws IOException, URISyntaxException, UnirestException {

    	gridNodeConfiguration.setStatus(GridStatus.ACTIVE);
    	JSONObject json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    	Assert.assertFalse(json.getString("driverVersion").isEmpty());
    	Assert.assertEquals(json.getInt("port"), 4444);
    	
    	Assert.assertEquals(json.getJSONArray("capabilities").getJSONObject(0).length(), 1);
    	Assert.assertEquals(json.getJSONArray("capabilities").getJSONObject(0).getString("browserName"), "chrome");
    }
    
    @Test(groups={"grid"})
    public void getJsonFormatFilterCapabilities() throws IOException, URISyntaxException, UnirestException {
    	
    	gridNodeConfiguration.setStatus(GridStatus.ACTIVE);
    	
    	MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
        caps.setCapability(CapabilityType.BROWSER_VERSION, "100.0");
        caps.setCapability(CapabilityType.PLATFORM_NAME, "Linux");
        caps.setCapability("maxInstances", 1);
        caps.setCapability("customCap", "capValue");
        when(gridNodeConfiguration.getCapabilities()).thenReturn(Arrays.asList(caps));
    	
    	JSONObject json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    	Assert.assertFalse(json.getString("driverVersion").isEmpty());
    	Assert.assertEquals(json.getInt("port"), 4444);
    	
    	Assert.assertEquals(json.getJSONArray("capabilities").getJSONObject(0).length(), 4); //customCap should not be there
    	Assert.assertTrue(json.getJSONArray("capabilities").getJSONObject(0).keySet().contains(CapabilityType.BROWSER_NAME));
    	Assert.assertTrue(json.getJSONArray("capabilities").getJSONObject(0).keySet().contains(CapabilityType.BROWSER_VERSION));
    	Assert.assertTrue(json.getJSONArray("capabilities").getJSONObject(0).keySet().contains(CapabilityType.PLATFORM_NAME));
    	Assert.assertTrue(json.getJSONArray("capabilities").getJSONObject(0).keySet().contains("maxInstances"));
    }
    
    /**
     * Check that with JSON format defined, json is returned
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void getJsonFormatWithUpdatedStatus() throws IOException, URISyntaxException, UnirestException {

    	gridNodeConfiguration.setStatus(GridStatus.INACTIVE);
    	
    	JSONObject json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "INACTIVE"); // default status is 'active'
    	Assert.assertEquals(json.getInt("port"), 4444); 
    }
    
    /**
     * Check we can set an activity status
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void setActivityStatus() throws IOException, URISyntaxException, UnirestException {
    	
    	gridNodeConfiguration.setStatus(GridStatus.ACTIVE);
    	JSONObject json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    	
    	Unirest.post(url).queryString("status", "inactive").asString();
    	
    	json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "INACTIVE"); // default status is 'active'
    }
    
    /**
     * Check we can not set an invalid activity status (error must be raised)
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void setInvalidActivityStatus() throws IOException, URISyntaxException, UnirestException {
    	
    	gridNodeConfiguration.setStatus(GridStatus.ACTIVE);
    	JSONObject json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    	
    	HttpResponse<String> response = Unirest.post(url).queryString("status", "running").asString();
    	Assert.assertEquals(response.getStatus(), 500);
    	
    	json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    }
    

 
}
