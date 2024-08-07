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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;

import com.infotel.seleniumrobot.grid.servlets.server.GridServlet;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.node.config.NodeOptions;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.server.NodeStatusServlet;
import com.infotel.seleniumrobot.grid.utils.GridStatus;

import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

import static org.mockito.Mockito.*;

public class TestNodeStatusServlet extends BaseServletTest {

    @Mock
    NodeOptions nodeOptions;
    
    @Mock
    BaseServerOptions serverOptions;
    
    GridNodeConfiguration gridNodeConfiguration;
    
    @InjectMocks
    NodeStatusServlet nodeServlet = new NodeStatusServlet();
    
	private MockedStatic mockedLaunchConfig;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {

		mockedLaunchConfig = mockStatic(LaunchConfig.class);
        
        gridNodeConfiguration = spy(new GridNodeConfiguration());
        
        LaunchConfig launchConfig = new LaunchConfig(new String[] {"node"});
		mockedLaunchConfig.when(() -> LaunchConfig.getCurrentNodeConfig()).thenReturn(gridNodeConfiguration);
		mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);
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
		mockedLaunchConfig.close();
    }

    /**
     * Check that with no format defined, HTML is returned
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void getHtmlFormat() throws IOException, URISyntaxException, UnirestException {
		GridServlet.ServletResponse response = nodeServlet.getStatus(null);
		String body = response.message;
    	
    	Assert.assertTrue(body.contains("<html>"));
    	Assert.assertTrue(body.contains("<div id=\"freeMemory\"> Free Memory: "));
    	Assert.assertTrue(body.contains("<h1 class=\"iframeTitre\" >System informations</h1>"));

		Assert.assertEquals(response.httpCode, 200);
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
		GridServlet.ServletResponse response = nodeServlet.getStatus("json");

    	JSONObject json = new JSONObject(response.message);
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    	Assert.assertFalse(json.getString("driverVersion").isEmpty());
    	Assert.assertEquals(json.getInt("port"), 4444);
    	
    	Assert.assertEquals(json.getJSONArray("capabilities").getJSONObject(0).length(), 1);
    	Assert.assertEquals(json.getJSONArray("capabilities").getJSONObject(0).getString("browserName"), "chrome");

		Assert.assertEquals(response.httpCode, 200);
    }
    
    @Test(groups={"grid"})
    public void getJsonFormatFilterCapabilities() throws IOException, URISyntaxException, UnirestException {
    	
    	gridNodeConfiguration.setStatus(GridStatus.ACTIVE);
    	
    	MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
        caps.setCapability(CapabilityType.BROWSER_VERSION, "100.0");
        caps.setCapability(CapabilityType.PLATFORM_NAME, "Linux");
        caps.setCapability("customCap", "capValue");
        when(gridNodeConfiguration.getCapabilities()).thenReturn(Arrays.asList(caps));

		GridServlet.ServletResponse response = nodeServlet.getStatus("json");

		JSONObject json = new JSONObject(response.message);
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    	Assert.assertFalse(json.getString("driverVersion").isEmpty());
    	Assert.assertEquals(json.getInt("port"), 4444);
    	
    	Assert.assertEquals(json.getJSONArray("capabilities").getJSONObject(0).length(), 3); //customCap should not be there
    	Assert.assertTrue(json.getJSONArray("capabilities").getJSONObject(0).keySet().contains(CapabilityType.BROWSER_NAME));
    	Assert.assertTrue(json.getJSONArray("capabilities").getJSONObject(0).keySet().contains(CapabilityType.BROWSER_VERSION));
    	Assert.assertTrue(json.getJSONArray("capabilities").getJSONObject(0).keySet().contains(CapabilityType.PLATFORM_NAME));
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

		GridServlet.ServletResponse response = nodeServlet.getStatus("json");

		JSONObject json = new JSONObject(response.message);
    	
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

		GridServlet.ServletResponse response = nodeServlet.getStatus("json");
		JSONObject json = new JSONObject(response.message);
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'

		nodeServlet.setStatus("inactive");

		response = nodeServlet.getStatus("json");
		json = new JSONObject(response.message);
    	
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
		GridServlet.ServletResponse response = nodeServlet.getStatus("json");
		JSONObject json = new JSONObject(response.message);
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'

		response = nodeServlet.setStatus("running");
    	Assert.assertEquals(response.httpCode, 500);

		response = nodeServlet.getStatus("json");
		json = new JSONObject(response.message);
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    }

	/**
	 * Test when no status is provided
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws UnirestException
	 */
	@Test(groups={"grid"})
    public void setNoActivityStatus() throws IOException, URISyntaxException, UnirestException {

		gridNodeConfiguration.setStatus(GridStatus.ACTIVE);
		GridServlet.ServletResponse response = nodeServlet.getStatus("json");
		JSONObject json = new JSONObject(response.message);

		Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'

		response = nodeServlet.setStatus(null);
		Assert.assertEquals(response.httpCode, 500);

		response = nodeServlet.getStatus("json");
		json = new JSONObject(response.message);

		Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    }
    

 
}
