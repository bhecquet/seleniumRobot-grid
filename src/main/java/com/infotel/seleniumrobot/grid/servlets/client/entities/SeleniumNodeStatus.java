package com.infotel.seleniumrobot.grid.servlets.client.entities;

import java.time.LocalDateTime;
import java.util.List;

import kong.unirest.json.JSONObject;

/**
 * Class representing a node status, as returned from /status endpoint
 * @author S047432
 *
 */
public class SeleniumNodeStatus {

	private boolean ready;
	private SeleniumNode node;
	
	/**
	 * For tests only
	 * @param ready
	 * @param node
	 */
	public SeleniumNodeStatus(boolean ready, SeleniumNode node) {
		this.ready = ready;
		this.node = node;
	}
	
	public SeleniumNodeStatus(JSONObject status) {
		ready = status.getJSONObject("value").optBoolean("ready", false);
		node = new SeleniumNode(status.getJSONObject("value").getJSONObject("node"));	
	}
	
	public boolean isReady() {
		return ready;
	}

	public boolean getAvailability() {
		return node.getAvailability();
	}

	public String getExternalUri() {
		return node.getExternalUri();
	}

	public int getMaxSessions() {
		return node.getMaxSessions();
	}

	public int getTestSlots() {
		return node.getTestSlots();
	}

	public boolean isBusy() {
		return node.isBusy();
	}

	public List<String> getSessionList() {
		return node.getSessionList();
	}

	public LocalDateTime getLastStarted() {
		return node.getLastStarted();
	}
}
