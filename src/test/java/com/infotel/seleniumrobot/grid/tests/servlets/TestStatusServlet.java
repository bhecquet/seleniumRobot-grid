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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.infotel.seleniumrobot.grid.servlets.client.INodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClientFactory;
import com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients.NodeStatusServletClientNodeDisappeared;
import com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients.NodeStatusServletClientNodeNotPresent;
import com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients.NodeStatusServletClientOk;
import com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients.NodeStatusServletClientSetStatusKo;
import org.apache.http.HttpHost;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.GridStatusClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNodeStatus;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import com.infotel.seleniumrobot.grid.servlets.server.StatusServlet;
import com.infotel.seleniumrobot.grid.utils.GridStatus;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

import static org.mockito.Mockito.*;

public class TestStatusServlet extends BaseServletTest {

    private Server nodeServer;
    private String url;

    private StatusServlet statusServlet;

    @Mock
    private LaunchConfig launchConfig;

	@Mock
	private GridStatusClient gridStatusClient;
	
	private SeleniumNode node1;
	private SeleniumNode node2;

	private MockedStatic mockedLaunchConfig;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
		NodeStatusServletClientOk.statusMap.clear();
		mockedLaunchConfig = mockStatic(LaunchConfig.class);

		mockedLaunchConfig.when(() -> LaunchConfig.getCurrentLaunchConfig()).thenReturn(launchConfig);



		node1 = new SeleniumNode(new JSONObject("{"
				+ "      \"availability\": \"UP\","
				+ "      \"externalUri\": \"http:\\u002f\\u002f127.0.0.1:5555\","
				+ "      \"heartbeatPeriod\": 60000,"
				+ "      \"maxSessions\": 3,"
				+ "      \"nodeId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "      \"osInfo\": {"
				+ "        \"arch\": \"amd64\","
				+ "        \"name\": \"Windows 10\","
				+ "        \"version\": \"10.0\""
				+ "      },"
				+ "      \"slots\": ["
				+ "        {"
				+ "          \"id\": {"
				+ "            \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "            \"id\": \"33e23352-d83c-486c-9e98-fa388e875951\""
				+ "          },"
				+ "          \"lastStarted\": \"1970-01-01T00:00:01Z\","
				+ "          \"session\": null,"
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"chrome\","
				+ "            \"browserVersion\": \"106.0\","
				+ "            \"sr:defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fLocal\\u002fGoogle\\u002fChrome\\u002fUser Data\","
				+ "            \"platform\": \"Windows 10\","
				+ "            \"platformName\": \"Windows 10\","
				+ "            \"sr:restrictToTags\": false,"
				+ "            \"se:webDriverExecutable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fchromedriver_105.0_chrome-105-106.exe\","
				+ "            \"sr:beta\": true,"
				+ "            \"sr:nodeTags\": ["
				+ "              \"toto\""
				+ "            ],"
				+ "            \"webdriver-executable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fchromedriver_105.0_chrome-105-106.exe\""
				+ "          }"
				+ "        }"
				+ "      ],"
				+ "      \"version\": \"4.2.2 (revision 683ccb65d6)\""
				+ "}"));
		node2 = new SeleniumNode(new JSONObject("{"
				+ "      \"availability\": \"UP\","
				+ "      \"externalUri\": \"http:\\u002f\\u002f127.0.0.1:5556\","
				+ "      \"heartbeatPeriod\": 60000,"
				+ "      \"maxSessions\": 3,"
				+ "      \"nodeId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63389\","
				+ "      \"osInfo\": {"
				+ "        \"arch\": \"amd64\","
				+ "        \"name\": \"Windows 10\","
				+ "        \"version\": \"10.0\""
				+ "      },"
				+ "      \"slots\": ["
				+ "        {"
				+ "          \"id\": {"
				+ "            \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "            \"id\": \"33e23352-d83c-486c-9e98-fa388e875951\""
				+ "          },"
				+ "          \"lastStarted\": \"1970-01-01T00:00:01Z\","
				+ "          \"session\": null,"
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"firefox\","
				+ "            \"browserVersion\": \"106.0\","
				+ "            \"max-sessions\": 5,"
				+ "            \"platform\": \"Windows 10\","
				+ "            \"platformName\": \"Windows 10\","
				+ "            \"sr:restrictToTags\": false,"
				+ "            \"se:webDriverExecutable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fgeckodriver_105.0_chrome-105-106.exe\","
				+ "            \"sr:beta\": true,"
				+ "            \"sr:nodeTags\": ["
				+ "              \"toto\""
				+ "            ],"
				+ "            \"webdriver-executable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fgeckodriver_105.0_chrome-105-106.exe\""
				+ "          }"
				+ "        }"
				+ "      ],"
				+ "      \"version\": \"4.2.2 (revision 683ccb65d6)\""
				+ "}"));
    }

	private void initServlet(Class<? extends INodeStatusServletClient> nodeStatusServletClientClass) throws Exception {
		statusServlet = new StatusServlet(gridStatusClient, new NodeStatusServletClientFactory(nodeStatusServletClientClass));
		nodeServer = startServerForServlet(statusServlet, "/" + StatusServlet.class.getSimpleName() + "/*");
		url = String.format("http://localhost:%d/StatusServlet/", ((ServerConnector)nodeServer.getConnectors()[0]).getLocalPort());
	}

	@AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
    	nodeServer.stop();

		mockedLaunchConfig.close();
    }

    /**
     * get hub status without node, only hub status is returned
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetHubStatusWithoutNode() throws Exception {

		initServlet(NodeStatusServletClientOk.class);
    	JSONObject json = Unirest.get(url).asJson().getBody().getObject();
    	Assert.assertEquals(json.getBoolean("success"), true);
    	Assert.assertEquals(json.length(), 2);
    }
 
    /**
     * test we get the node status 
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetHubStatusWithNode() throws Exception {

		when(gridStatusClient.getNodes()).thenReturn(Arrays.asList(node1));

		initServlet(NodeStatusServletClientOk.class);

		JSONObject json = Unirest.get(url).asJson().getBody().getObject();
		Assert.assertEquals(json.getBoolean("success"), true);
		Assert.assertEquals(json.length(), 3);

		String nodeId = "http://127.0.0.1:5555";
		Assert.assertTrue(json.has(nodeId));
		Assert.assertEquals(json.getJSONObject(nodeId).getString("version"), "5.1.0");
		Assert.assertEquals(json.getJSONObject(nodeId).getString("driverVersion"), "5.0.0");
		Assert.assertEquals(json.getJSONObject(nodeId).getString("lastSessionStart"), "1970-01-01T00:00:01");
		Assert.assertEquals(json.getJSONObject(nodeId).getBoolean("busy"), false);
		Assert.assertEquals(json.getJSONObject(nodeId).getInt("testSlots"), 1);
		Assert.assertEquals(json.getJSONObject(nodeId).getInt("usedTestSlots"), 0);
		Assert.assertEquals(json.getJSONObject(nodeId).getString(StatusServlet.STATUS), "ACTIVE");

    }

    
    /**
     * issue #28: do not display node status if connection problem occurs in hub, getting the node status
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testStatusWithNodeDisappeared() throws Exception {

		initServlet(NodeStatusServletClientNodeDisappeared.class);

		JSONObject json = Unirest.get(url).asJson().getBody().getObject();

		String nodeId = "http://127.0.0.1:5555";
		Assert.assertFalse(json.has(nodeId));
    }



    @Test(groups={"grid"})
    public void testStatusWithNodeDisappeared2() throws Exception {

		initServlet(NodeStatusServletClientNodeNotPresent.class);
		JSONObject json = Unirest.get(url).asJson().getBody().getObject();

		String nodeId = "http://127.0.0.1:5555";
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
    public void testGetDirectNodeActiveStatus() throws Exception {

		when(gridStatusClient.getNodes()).thenReturn(Arrays.asList(node1));
		initServlet(NodeStatusServletClientOk.class);
		String nodeId = "http://127.0.0.1:5555";
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
    public void testGetFullNodeStatus() throws Exception {
		when(gridStatusClient.getNodes()).thenReturn(Arrays.asList(node1));
		initServlet(NodeStatusServletClientOk.class);
		String nodeId = "http://127.0.0.1:5555";
		JSONObject json = Unirest.get(url)
				.queryString("jsonpath", String.format("$['%s']", nodeId))
				.asJson().getBody().getObject();
		Assert.assertEquals(json.getString("version"), "5.1.0");
    }
    
    /**
     * test we get null using invalid jsonPath (https://github.com/json-path/JsonPath)
     * We use the bracket notation as nodeId contains '.' and this leads to error in searching path
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testGetNullWithInvalidJsonPathToNodeStatus() throws Exception {
		when(gridStatusClient.getNodes()).thenReturn(Arrays.asList(node1));
		initServlet(NodeStatusServletClientOk.class);

		String reply = Unirest.get(url)
				.queryString("jsonpath", "$['invalid']")
				.asString().getBody();
		Assert.assertEquals(reply, "null");
    }

    
    /**
     * Set hub to inactive 
     * check node has been set to inactive
     * 
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testSetHubStatus() throws Exception {
		when(gridStatusClient.getNodes()).thenReturn(Arrays.asList(node1, node2));
		initServlet(NodeStatusServletClientOk.class);

		HttpResponse<String> reply = Unirest.post(url)
				.queryString(StatusServlet.STATUS, GridStatus.INACTIVE.toString())
				.asString();

		Assert.assertEquals(reply.getStatus(), 200);

		// check node has also been configured as inactive (once for each node)
		Assert.assertEquals(NodeStatusServletClientOk.statusMap.size(), 2);
		NodeStatusServletClientOk.statusMap.forEach((k, v) -> Assert.assertEquals(v, GridStatus.INACTIVE));
    }
    
    /**
     * Check that only active & inactive states are allowed
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testSetUnknwonHubStatus() throws Exception {
		initServlet(NodeStatusServletClientOk.class);
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
    public void testSetInvalidHubStatus() throws Exception {
		initServlet(NodeStatusServletClientOk.class);
		HttpResponse<String> reply = Unirest.post(url)
				.queryString(StatusServlet.STATUS, "invalid")
				.asString();

		Assert.assertEquals(reply.getStatus(), 500);
    }


    
    /**
     * Check that if an error occurs setting node status, service returns error
     * @throws IOException
     * @throws URISyntaxException
     * @throws UnirestException
     */
    @Test(groups={"grid"})
    public void testSetNodeStatusRaisesError() throws Exception {

		when(gridStatusClient.getNodes()).thenReturn(Arrays.asList(node1));
		initServlet(NodeStatusServletClientSetStatusKo.class);

		HttpResponse<String> reply = Unirest.post(url)
				.queryString(StatusServlet.STATUS, GridStatus.INACTIVE.toString())
				.asString();

		Assert.assertEquals(reply.getStatus(), 500);
    }
    
    /**
     * issue #49: test that when error is raised connecting to node, hub status replies OK
     */
    @Test(groups={"grid"})
    public void testGetNodeStatusRaisesError() throws Exception {

		initServlet(NodeStatusServletClientNodeNotPresent.class);
		when(gridStatusClient.getNodes()).thenReturn(Arrays.asList(node1));

		HttpResponse<String> reply = Unirest.get(url)
				.asString();

		Assert.assertEquals(reply.getStatus(), 200);
    }
}
