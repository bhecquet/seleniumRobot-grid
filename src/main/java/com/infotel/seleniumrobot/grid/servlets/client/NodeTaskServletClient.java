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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import kong.unirest.Unirest;
import kong.unirest.UnirestException;

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
    		logger.warn("Could not restart node");
    	}
	}
	
	/**
	 * Stop the node
	 * @throws UnirestException
	 */
	public void stopNode() throws UnirestException {
		Unirest.post(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("action", "stop")
				.asString();
	}
	
	/**
	 * Stop video capture
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnirestException 
	 */
	public void stopVideoCapture(String sessionId) throws UnirestException {
		Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("action", "stopVideoCapture")
				.queryString("session", sessionId).asBytes();
	}
	
	public String startAppium(String sessionId) throws UnirestException {
		return Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("action", "startAppium")
				.queryString("session", sessionId).asString().getBody();
	}
	
	public void stopAppium(String sessionId) throws UnirestException {
		Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("action", "stopAppium")
				.queryString("session", sessionId).asString();
	}
	
	public void setProperty(String key, String value) throws UnirestException {
		Unirest.post(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
			.queryString("action", "setProperty")
			.queryString("key", key)
			.queryString("value", value)
			.asString();
	}
	
	/**
	 * REturns list of PIDS for this driver exclusively
	 * @param browserName
	 * @param browserVersion
	 * @param existingPids		existing pids for the same driver. They will be filtered out in the reply
	 * @return
	 * @throws UnirestException
	 */
	public List<Long> getDriverPids(String browserName, String browserVersion, List<Long> existingPids) throws UnirestException {
		String pidList = Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
						.queryString("action", "driverPids")
						.queryString("browserName", browserName)
						.queryString("browserVersion", browserVersion)
						.queryString("existingPids", StringUtils.join(existingPids, ","))
						.asString()
						.getBody();
		
		if (pidList.isEmpty()) {
			return new ArrayList<>();
		} else {
			return Arrays.asList(pidList
					.split(","))
					.stream()
					.map(Long::parseLong)
					.collect(Collectors.toList())
					;
		}
	}
	
	/**
	 * Returns list of PIDs for this driver and for all subprocess created (driver, browser and other processes)
	 * @param browserName
	 * @param browserVersion
	 * @param parentPids		pids of the driver. We'll look for browser created by this driver
	 * @return
	 * @throws UnirestException
	 */
	public List<Long> getBrowserAndDriverPids(String browserName, String browserVersion, List<Long> parentPids) throws UnirestException {
		String pidList = Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
						.queryString("action", "browserAndDriverPids")
						.queryString("browserName", browserName)
						.queryString("browserVersion", browserVersion)
						.queryString("parentPids", StringUtils.join(parentPids, ","))
						.asString()
						.getBody();
		if (pidList.isEmpty()) {
			return new ArrayList<>();
		} else {
			return Arrays.asList(pidList
					.split(","))
					.stream()
					.map(Long::parseLong)
					.collect(Collectors.toList())
					;
		}
	}
	
	public void killProcessByPid(Long pid) throws UnirestException {
		Unirest.post(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("action", "killPid")
				.queryString("pid", pid.toString()).asString();
	}
	
	public void keepAlive() throws UnirestException {
		Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
		.queryString("action", "keepAlive")
		.asString();
	}
	
	public void cleanNode() throws UnirestException {
		Unirest.post(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
		.queryString("action", "clean")
		.asString();
	}
}
