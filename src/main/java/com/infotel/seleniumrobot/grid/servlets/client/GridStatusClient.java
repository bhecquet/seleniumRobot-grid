package com.infotel.seleniumrobot.grid.servlets.client;

import java.net.URI;
import java.net.URL;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Class allowing to get info from "/status" of grid
 */
public class GridStatusClient {
	
	private String gridUrl;

	/**
	 * The grid router URL, typically http://localhost:4444
	 * @param gridUrl
	 */
	public GridStatusClient(URL gridUrl) {
		this.gridUrl = gridUrl.toString() + "/status";
	}
	public GridStatusClient(URI gridUrl) {
		this.gridUrl = gridUrl.toString() + "/status";
	}
	
	private JSONObject getStatus() {
		
		JsonNode status = Unirest.get(gridUrl).asJson().getBody();
		return status.getObject().getJSONObject("value");
	}
	
	public boolean isReady() {
		return getStatus().optBoolean("ready", false);
	}
	
	public JSONArray getNodes() {
		JSONArray nodes = getStatus().optJSONArray("nodes");
		return nodes == null ? new JSONArray(): nodes;
	}
}
