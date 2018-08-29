package com.infotel.seleniumrobot.grid.servlets.client;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class NodeStatusServletClient {
	
	private static final Logger logger = Logger.getLogger(NodeStatusServletClient.class.getName());
	private static final String SERVLET_PATH = "/extra/NodeStatusServlet/";
	
	private HttpHost httpHost;
	
	public NodeStatusServletClient(String host, int port) {
        this.httpHost = new HttpHost(host, port);
    }	

	/**
	 * Returns the json status
	 * @param sessionId
	 * @throws UnirestException
	 */
	public JSONObject getStatus() throws UnirestException {
		return Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("format", "json")
				.asJson().getBody().getObject();
	}
	
	public void setStatus(GridStatus newStatus) throws UnirestException {
		HttpResponse<String> response = Unirest.post(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("status", newStatus.toString())
				.asString();
		if (response.getStatus() != 200) {
			throw new SeleniumGridException(String.format("could not set status %s on node", newStatus.toString()));
		}
	}
}
