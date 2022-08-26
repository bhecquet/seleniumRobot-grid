package com.infotel.seleniumrobot.grid.servlets.client;

import java.net.URI;
import java.net.URL;

import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

/**
 * Class allowing to perform actions on a node as defined here https://www.selenium.dev/documentation/grid/advanced_features/endpoints/#drain-node
 */
public class NodeClient {
	
	private String nodeUrl;

	/**
	 * The grid  URL, typically http://localhost:4444 (router) or http://localhost:4444 (node)
	 * @param nodeUrl
	 */
	public NodeClient(URL nodeUrl) {
		this.nodeUrl = nodeUrl.toString();
	}
	public NodeClient(URI nodeUrl) {
		this.nodeUrl = nodeUrl.toString();
	}
	
	public void drainNode() {
		
		HttpResponse response = Unirest.post(nodeUrl + "/se/grid/node/drain").header("X-REGISTRATION-SECRET", "").asEmpty();
		if (response.isSuccess()) {
			throw new SeleniumGridException("Could not drain node");
		}
	}
}
