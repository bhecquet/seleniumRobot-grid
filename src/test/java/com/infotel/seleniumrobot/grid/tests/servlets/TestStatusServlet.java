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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.json.JSONObject;
import org.mockito.Mock;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.grid.web.Hub;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.CustomRemoteProxy;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.StatusServlet;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@PrepareForTest({})
@PowerMockIgnore({"javax.net.ssl.*", // to avoid error java.security.NoSuchAlgorithmException: class configured for SSLContext: sun.security.ssl.SSLContextImpl$TLS10Context not a SSLContext
				"javax.management.*"}) // to avoid error: java.lang.LinkageError: loader constraint violation: loader (instance of org/powermock/core/classloader/MockClassLoader) previously initiated loading for a different type with name "javax/management/MBeanServer"
public class TestStatusServlet extends BaseServletTest {

    private Server nodeServer;
    private String url;
    private ProxySet proxySet;
	private JSONObject nodeStatus;

	private GridNodeConfiguration nodeConfig = new GridNodeConfiguration();
    private GridHubConfiguration gridHubConfiguration;
    
    @Mock
    GridRegistry registry;
	private RegistrationRequest request = RegistrationRequest.build(nodeConfig);
    private StatusServlet statusServlet;
	
	@Mock
	private NodeTaskServletClient nodeClient;
	
	@Mock
	private NodeStatusServletClient nodeStatusClient;

	@Mock
	private MobileNodeServletClient mobileServletClient;
 
    private CustomRemoteProxy remoteProxy;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
    	gridHubConfiguration = new GridHubConfiguration();
    	Hub hub = new Hub(gridHubConfiguration);
    	when(registry.getHub()).thenReturn(hub);
    	
    	statusServlet = new StatusServlet(registry);
        nodeServer = startServerForServlet(statusServlet, "/" + StatusServlet.class.getSimpleName() + "/*");
        url = String.format("http://localhost:%d/StatusServlet/", ((ServerConnector)nodeServer.getConnectors()[0]).getLocalPort());

        
        remoteProxy = spy(new CustomRemoteProxy(request, registry, nodeClient, nodeStatusClient, mobileServletClient, 5));
        doReturn(new HashMap<>()).when(remoteProxy).getProxyStatus();

		nodeStatus = new JSONObject("{\r\n" + 
					"  \"memory\": {\r\n" + 
					"    \"totalMemory\": 17054,\r\n" + 
					"    \"class\": \"com.infotel.seleniumrobot.grid.utils.MemoryInfo\",\r\n" + 
					"    \"freeMemory\": 5035\r\n" + 
					"  },\r\n" + 
					"  \"maxSessions\": 1,\r\n" + 
					"  \"port\": 5554,\r\n" + 
					"  \"ip\": \"SN782980\",\r\n" + 
					"  \"cpu\": 0.0,\r\n" + 
					"  \"version\": \"3.14.0-SNAPSHOT\",\r\n" + 
					"  \"status\": \"ACTIVE\"\r\n" + 
					"}");
		

		when(nodeStatusClient.getStatus()).thenReturn(nodeStatus);
        
        proxySet = new ProxySet(false);
        when(registry.getAllProxies()).thenReturn(proxySet);
    }

    @AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
    	nodeServer.stop();
    }

    /**
     * get hub status without node, only hub status is returned
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetHubStatusWithoutNode() throws IOException, URISyntaxException, UnirestException {
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();
    	Assert.assertEquals(json.getBoolean("success"), true);
    	Assert.assertEquals(json.getJSONObject("hub").getString(StatusServlet.STATUS), "ACTIVE");
    	Assert.assertEquals(json.length(), 2);
    }
    
    /**
     * get hub status without node, activity status is defined
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetHubStatusIfDefined() throws IOException, URISyntaxException, UnirestException {
    	gridHubConfiguration.custom.put(StatusServlet.STATUS, GridStatus.INACTIVE.toString());
    	
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();
    	Assert.assertEquals(json.getBoolean("success"), true);
    	Assert.assertEquals(json.getJSONObject("hub").getString(StatusServlet.STATUS), "INACTIVE");
    	Assert.assertEquals(json.length(), 2);
    }
 
    /**
     * test we get the node status 
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetHubStatusWithNode() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);
    	
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();
    	Assert.assertEquals(json.getBoolean("success"), true);
    	Assert.assertEquals(json.getJSONObject("hub").getString(StatusServlet.STATUS), "ACTIVE");
    	Assert.assertEquals(json.length(), 3);
    	
    	String nodeId = String.format("http://%s:%s", nodeConfig.host, nodeConfig.port);
    	Assert.assertTrue(json.has(nodeId));
    	Assert.assertEquals(json.getJSONObject(nodeId).getString("version"), "3.14.0-SNAPSHOT");
    	Assert.assertEquals(json.getJSONObject(nodeId).getString(StatusServlet.STATUS), "ACTIVE");
    }
    
    /**
     * test node 'lastSessionStart' value is 'never' if not test session has occured 
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testLastSessionStartWithNoSessionBefore() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);
    	
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();

    	String nodeId = String.format("http://%s:%s", nodeConfig.host, nodeConfig.port);
    	Assert.assertTrue(json.has(nodeId));
    	Assert.assertEquals(json.getJSONObject(nodeId).getString("lastSessionStart"), "never");
    }
    
    /**
     * test node 'lastSessionStart' value is a human readable date if test session has occured 
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testLastSessionStartWithSessionBefore() throws IOException, URISyntaxException, UnirestException {
    	
    	long currentTime = 1535641545302L;
    	
    	proxySet.add(remoteProxy);
    	when(remoteProxy.getLastSessionStart()).thenReturn(currentTime);
    	
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();
    	
    	String nodeId = String.format("http://%s:%s", nodeConfig.host, nodeConfig.port);
    	Assert.assertTrue(json.has(nodeId));
    	Assert.assertTrue(json.getJSONObject(nodeId).getString("lastSessionStart").startsWith("2018-08-30T"));
    }
    
    /**
     * issue #28: do not display node status if connection problem occurs in hub, getting the proxy status
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testStatusWithNodeDisappeared() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);
    	
    	doThrow(GridException.class).when(remoteProxy).getProxyStatus();
    	
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();

    	String nodeId = String.format("http://%s:%s", nodeConfig.host, nodeConfig.port);
    	Assert.assertFalse(json.has(nodeId));
    }
    
    /**
     * test we get the node 'active' status directly using jsonPath (https://github.com/json-path/JsonPath)
     * We use the bracket notation as nodeId contains '.' and this leads to error in searching path
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetDirectNodeActiveStatus() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);
    	String nodeId = String.format("http://%s:%s", nodeConfig.host, nodeConfig.port);
    	
    	String reply = Unirest.get(url)
    			.queryString("jsonpath", String.format("$['%s']['status']", nodeId))
    			.asString().getBody();
    	Assert.assertEquals(reply, "ACTIVE");
    }
    
    /**
     * test we get the node status directly using jsonPath (https://github.com/json-path/JsonPath)
     * We use the bracket notation as nodeId contains '.' and this leads to error in searching path
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetFullNodeStatus() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);
    	String nodeId = String.format("http://%s:%s", nodeConfig.host, nodeConfig.port);
    	
    	JSONObject json = Unirest.get(url)
    			.queryString("jsonpath", String.format("$['%s']", nodeId))
    			.asJson().getBody().getObject();
    	Assert.assertEquals(json.getString("version"), "3.14.0-SNAPSHOT");
    }
    
    /**
     * test we get null using invalid jsonPath (https://github.com/json-path/JsonPath)
     * We use the bracket notation as nodeId contains '.' and this leads to error in searching path
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetNullWithInvalidJsonPathToNodeStatus() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);

    	String reply = Unirest.get(url)
    			.queryString("jsonpath", "$['invalid']")
    			.asString().getBody();
    	Assert.assertEquals(reply, "null");
    }
    
    /**
     * test we get the node status 
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetHubStatusWithInactiveNode() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);
    	
    	nodeStatus.put(StatusServlet.STATUS, GridStatus.INACTIVE.toString());
    	
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();
    	Assert.assertEquals(json.getBoolean("success"), true);
    	Assert.assertEquals(json.getJSONObject("hub").getString(StatusServlet.STATUS), "ACTIVE");
    	Assert.assertEquals(json.length(), 3);
    	
    	String nodeId = String.format("http://%s:%s", nodeConfig.host, nodeConfig.port);
    	Assert.assertTrue(json.has(nodeId));
    	Assert.assertEquals(json.getJSONObject(nodeId).getString("version"), "3.14.0-SNAPSHOT");
    	Assert.assertEquals(json.getJSONObject(nodeId).getString(StatusServlet.STATUS), "INACTIVE");
    }
    
    /**
     * Set hub to inactive 
     * check configuration is kept and each node is also inactive
     * 
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testSetHubStatus() throws IOException, URISyntaxException, UnirestException {
    	
    	proxySet.add(remoteProxy);
    	
    	Assert.assertEquals(gridHubConfiguration.custom.get(StatusServlet.STATUS), null);
    	HttpResponse<String> reply = Unirest.post(url)
    			.queryString(StatusServlet.STATUS, GridStatus.INACTIVE.toString())
    			.asString();

    	Assert.assertEquals(reply.getStatus(), 200);

    	gridHubConfiguration.custom.put(StatusServlet.STATUS, GridStatus.INACTIVE.toString());
    	
    	// check hub is inactive
    	Assert.assertEquals(gridHubConfiguration.custom.get(StatusServlet.STATUS), GridStatus.INACTIVE.toString());
    	
    	// check node has also been configured as inactive
    	verify(nodeStatusClient).setStatus(GridStatus.INACTIVE);
    	
    	reply = Unirest.post(url)
    			.queryString(StatusServlet.STATUS, "active")
    			.asString();
    	Assert.assertEquals(gridHubConfiguration.custom.get(StatusServlet.STATUS), GridStatus.ACTIVE.toString());
    }
    
    /**
     * Check that only active & inactive states are allowed
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testSetUnknwonHubStatus() throws IOException, URISyntaxException, UnirestException {
    	HttpResponse<String> reply = Unirest.post(url)
    			.queryString(StatusServlet.STATUS, GridStatus.UNKNOWN.toString())
    			.asString();
    	
    	Assert.assertEquals(reply.getStatus(), 500);
    }
    
    /**
     * Check that only active & inactive states are allowed
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testSetInvalidHubStatus() throws IOException, URISyntaxException, UnirestException {
    	HttpResponse<String> reply = Unirest.post(url)
    			.queryString(StatusServlet.STATUS, "invalid")
    			.asString();

    	Assert.assertEquals(reply.getStatus(), 500);
    }
    
    /**
     * Check that only active & inactive states are allowed
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testSetNodeStatusRaisesError() throws IOException, URISyntaxException, UnirestException {
    	doThrow(SeleniumGridException.class).when(nodeStatusClient).setStatus(GridStatus.INACTIVE);
    	proxySet.add(remoteProxy);
    	
    	HttpResponse<String> reply = Unirest.post(url)
    			.queryString(StatusServlet.STATUS, GridStatus.INACTIVE.toString())
    			.asString();
    	
    	Assert.assertEquals(reply.getStatus(), 500);
    }
    
    /**
     * issue #49: test that when error is raised connecting to node, hub status replies OK
     */
    @Test(groups={"grid"})
    public void testGetNodeStatusRaisesError() throws IOException, URISyntaxException, UnirestException {
    	doThrow(SeleniumGridException.class).when(nodeStatusClient).getStatus();
    	proxySet.add(remoteProxy);
    	
    	HttpResponse<String> reply = Unirest.get(url)
    			.asString();
    	
    	Assert.assertEquals(reply.getStatus(), 200);
    }
}
