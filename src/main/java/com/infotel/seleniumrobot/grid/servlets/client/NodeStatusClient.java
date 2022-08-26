package com.infotel.seleniumrobot.grid.servlets.client;

import java.net.URI;
import java.net.URL;
import java.util.List;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Class allowing to get info from "/status" of grid
 */
public class NodeStatusClient {
	
	private String nodeUrl;

	/**
	 * The grid node URL, typically http://localhost:5555
	 * @param gridUrl
	 */
	public NodeStatusClient(URL gridUrl) {
		this.nodeUrl = gridUrl.toString() + "/status";
	}
	public NodeStatusClient(URI gridUrl) {
		this.nodeUrl = gridUrl.toString() + "/status";
	}
	
	private JSONObject getStatus() {
		
		JsonNode status = Unirest.get(nodeUrl).asJson().getBody();
		return status.getObject().getJSONObject("value");
	}
	
	public boolean isReady() {
		return getStatus().optBoolean("ready", false);
	}
	
	/**
	 * Check whether the node has one of its slots with an active session
	 * @return
	 */
	public boolean isBusy() {
		JSONArray slots = getStatus().getJSONObject("node").optJSONArray("slots");
		for (JSONObject slot: (List<JSONObject>)slots.toList()) {
			if (slot.getString("session") != null) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check whether the node has one of its slots with an active session
	 * @param sessionIdToExclude	the sessuib ti exclude from search
	 * @return
	 */
	public boolean isBusyOnOtherSlot(String sessionIdToExclude) {
		JSONArray slots = getStatus().getJSONObject("node").optJSONArray("slots");
		for (JSONObject slot: (List<JSONObject>)slots.toList()) {
			if (slot.optJSONObject("session") != null && slot.getJSONObject("session").getString("sessionId") != sessionIdToExclude) {
				return true;
			}
		}
		return false;
	}
	
	public JSONArray getSlots() {
		JSONArray nodes = getStatus().optJSONArray("nodes");
		return nodes == null ? new JSONArray(): nodes;
	}
}
