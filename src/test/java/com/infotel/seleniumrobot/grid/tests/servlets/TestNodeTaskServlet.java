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
import java.nio.charset.Charset;

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
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.infotel.seleniumrobot.grid.tasks.NodeRestartTask;
import com.infotel.seleniumrobot.grid.utils.Utils;


public class TestNodeTaskServlet extends BaseServletTest {

    private Server nodeServer;
    private HttpHost serverHost;
    
    @Mock
    NodeRestartTask restartTask;
    
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
    
 
}
