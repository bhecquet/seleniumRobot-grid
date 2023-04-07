package com.infotel.seleniumrobot.grid.servlets.client;

import java.net.URL;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import com.infotel.seleniumrobot.grid.utils.GridStatus;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

/**
 * Client for NodeStatusServlet
 * @author S047432
 *
 */
public class NodeStatusServletClient {
	
	private static final Logger logger = LogManager.getLogger(NodeStatusServletClient.class.getName());
	private static final String SERVLET_PATH = "/extra/NodeStatusServlet";
	
	private HttpHost httpHost;
	
	/**
	 * @param host	host of node
	 * @param port	port of node (the one defined at startup, not the servlet port)
	 */
	public NodeStatusServletClient(String host, int port) {
        this.httpHost = new HttpHost(host, port + 10);
    }	
	
	/**
	 * URL of the node
	 * @param url
	 */
	public NodeStatusServletClient(URL url) {
		this.httpHost = new HttpHost(url.getHost(), url.getPort() + 10);
	}	

	/**
	 * Returns the json status
	 * @param sessionId
	 * @throws UnirestException
	 */
	public SeleniumRobotNode getStatus() throws UnirestException {
		HttpResponse<JsonNode> response = Unirest.get(String.format("%s%s", httpHost.toURI().toString(), SERVLET_PATH))
				.queryString("format", "json")
				.asJson();
		if (response.getStatus() != 200) {
			throw new SeleniumGridException(String.format("could not get status from node: %s", response.getStatusText()));
		}
		return SeleniumRobotNode.fromJson(response.getBody().getObject());
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
