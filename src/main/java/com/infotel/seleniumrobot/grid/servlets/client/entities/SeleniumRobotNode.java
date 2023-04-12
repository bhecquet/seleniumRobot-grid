package com.infotel.seleniumrobot.grid.servlets.client.entities;

import kong.unirest.json.JSONObject;

/**
 * Class representing a node from information returned by NodeStatusServlet
 * @author S047432
 *
 */
public class SeleniumRobotNode {

	private String version;
	private String driverVersion;
	private String nodeStatus;
	private int maxSessions;

	public SeleniumRobotNode(String status, int maxSessions, String version, String driverVersion) {
		this.nodeStatus = status;
		this.maxSessions = maxSessions;
		this.version = version;
		this.driverVersion = driverVersion;
	}
	
	public String getVersion() {
		return version;
	}

	public String getDriverVersion() {
		return driverVersion;
	}

	public String getNodeStatus() {
		return nodeStatus;
	}

	public int getMaxSessions() {
		return maxSessions;
	}
	
	public static SeleniumRobotNode fromJson(JSONObject jsonStatus) {
		return new SeleniumRobotNode(jsonStatus.optString("status", "unknown"),
				jsonStatus.optInt("maxSessions", 0),
				jsonStatus.optString("version", "unknown"),
				jsonStatus.optString("driverVersion", "unknown"));

	}
}
