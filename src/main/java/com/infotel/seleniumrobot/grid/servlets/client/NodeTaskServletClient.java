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
package com.infotel.seleniumrobot.grid.servlets.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

public class NodeTaskServletClient {
	
	private final HttpHost httpHost;
	
	private static final String SERVLET_PATH = "/extra/NodeTaskServlet/";
	private static final Logger logger = Logger.getLogger(NodeTaskServletClient.class.getName());
	
	public NodeTaskServletClient(String host, int port) {
		httpHost = new HttpHost(host, port);
	}

	public void restart() throws ClientProtocolException, IOException, URISyntaxException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder();
		
    	builder.setPath(SERVLET_PATH);
    	builder.setParameter("action", "restart");
    	
    	HttpPost httpPost = new HttpPost(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(httpHost, httpPost);
    	
    	// update caps only if reply is OK
    	// reply can be KO (404 error) if no mobile capability is found
    	if (execute.getStatusLine().getStatusCode() != 200) {    	
    		logger.warning("Could not restart node");
    	}
	}
	
	/**
	 * Get version of the node
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String getVersion() throws ClientProtocolException, IOException, URISyntaxException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder();
		
		builder.setPath(SERVLET_PATH);
		builder.setParameter("action", "version");
		
		HttpGet httpGet = new HttpGet(builder.build());
		CloseableHttpResponse execute = httpClient.execute(httpHost, httpGet);

		if (execute.getStatusLine().getStatusCode() != 200) {    	
			return null;
		} else {
			JSONObject reply = new JSONObject(IOUtils.toString(execute.getEntity().getContent()));
			return reply.getString("version");
		}
	}
}
