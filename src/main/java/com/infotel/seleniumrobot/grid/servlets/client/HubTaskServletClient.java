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

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class HubTaskServletClient {
	
	private final HttpHost httpHost;
	
	private static final String SERVLET_PATH = "/grid/admin/HubTaskServlet";
	private static final Logger logger = Logger.getLogger(HubTaskServletClient.class.getName());
	
	public HubTaskServletClient(String host, int port) {
		logger.info("Hub host: " + host);
		httpHost = new HttpHost(host, port);
	}


	/**
	 * disable timeout for the session
	 * Useful when executing long commands
	 * @throws UnirestException
	 */
	public void disableTimeout(String session) throws UnirestException {
		logger.info(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH));
		Unirest.post(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("action", "ignoreTimeout")
				.queryString("ignore", "true")
				.queryString("session", session)
				.asString();
	}
	
	/**
	 * disable timeout for the session
	 * Useful when executing long commands
	 * @throws UnirestException
	 */
	public void enableTimeout(String session) throws UnirestException {
		Unirest.post(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
		.queryString("action", "ignoreTimeout")
		.queryString("ignore", "false")
		.queryString("session", session)
		.asString();
	}
	
	public void keepDriverAlive(String session) throws UnirestException {
		Unirest.get(String.format("%s/wd/hub/session/%s/url", httpHost.toURI().toString(), session))
		.asString();
	}
	
}
