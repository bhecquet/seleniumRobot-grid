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
import com.infotel.seleniumrobot.grid.servlets.server.NodeStatusServlet;
import com.infotel.seleniumrobot.grid.servlets.server.StatusServlet;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@PrepareForTest({LaunchConfig.class})
@PowerMockIgnore({"javax.net.ssl.*", // to avoid error java.security.NoSuchAlgorithmException: class configured for SSLContext: sun.security.ssl.SSLContextImpl$TLS10Context not a SSLContext
				"javax.management.*"}) // to avoid error: java.lang.LinkageError: loader constraint violation: loader (instance of org/powermock/core/classloader/MockClassLoader) previously initiated loading for a different type with name "javax/management/MBeanServer"
public class TestNodeStatusServlet extends BaseServletTest {

    private Server nodeServer;
    private String url;
    
    @Mock
    GridRegistry registry;

    GridNodeConfiguration gridNodeConfiguration;

    @InjectMocks
    NodeStatusServlet nodeServlet = new NodeStatusServlet(registry);

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
        nodeServer = startServerForServlet(nodeServlet, "/" + NodeStatusServlet.class.getSimpleName() + "/*");
        url = String.format("http://localhost:%d/NodeStatusServlet/", ((ServerConnector)nodeServer.getConnectors()[0]).getLocalPort());
        
        PowerMockito.mockStatic(LaunchConfig.class);
        
        gridNodeConfiguration = new GridNodeConfiguration();
        when(LaunchConfig.getCurrentNodeConfig()).thenReturn(gridNodeConfiguration);
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
    	JSONObject json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "ACTIVE"); // default status is 'active'
    	Assert.assertEquals(json.getInt("port"), -1); 
    }
    
    /**
     * Check that with JSON format defined, json is returned
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void getJsonFormatWithUpdatedStatus() throws IOException, URISyntaxException, UnirestException {
    	
    	gridNodeConfiguration.custom.put(StatusServlet.STATUS, GridStatus.INACTIVE.toString());
    	
    	JSONObject json = Unirest.get(url)
    			.queryString("format", "json")
    			.asJson()
    			.getBody()
    			.getObject();
    	
    	Assert.assertEquals(json.getString("status"), "INACTIVE"); // default status is 'active'
    	Assert.assertEquals(json.getInt("port"), -1); 
    }
    
    /**
     * Check we can set an activity status
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void setActivityStatus() throws IOException, URISyntaxException, UnirestException {
    	
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
