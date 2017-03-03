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
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.openqa.grid.common.exception.CapabilityNotPresentOnTheGridException;
import org.openqa.selenium.remote.DesiredCapabilities;

public class MobileNodeServletClient {
	
	private final HttpHost httpHost;
	
	public MobileNodeServletClient(String host, int port) {
		httpHost = new HttpHost(host, port);
	}

	public DesiredCapabilities updateCapabilities(DesiredCapabilities caps) throws ClientProtocolException, IOException, URISyntaxException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder();
		
    	builder.setPath("/extra/MobileNodeServlet/");
    	for (Entry<String, ?> entry: caps.asMap().entrySet()) {
    		builder.setParameter(entry.getKey(), entry.getValue().toString());
    	}
    	
    	HttpGet httpGet = new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(httpHost, httpGet);
    	
    	// update caps only if reply is OK
    	// reply can be KO (404 error) if no mobile capability is found
    	if (execute.getStatusLine().getStatusCode() == 200) {    	
    		JSONObject reply = new JSONObject(IOUtils.toString(execute.getEntity().getContent()));
    		DesiredCapabilities newCaps = new DesiredCapabilities();
        	for (Entry<String, ?> entry: caps.asMap().entrySet()) {
        		newCaps.setCapability(entry.getKey(), reply.getString(entry.getKey()));
        	}
        	httpClient.close();
        	return newCaps;
    	} else {
    		throw new CapabilityNotPresentOnTheGridException("No mobile device has been found with these capabilities: " + caps);
    	}
    	
    	
    	
    	
    	
	}
}
