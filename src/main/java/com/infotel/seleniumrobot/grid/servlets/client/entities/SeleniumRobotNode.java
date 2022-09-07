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
		SeleniumRobotNode status = new SeleniumRobotNode();
		status.version = jsonStatus.optString("version", "unknown");
		status.maxSessions = jsonStatus.optInt("maxSessions", 0);
		status.nodeStatus = jsonStatus.optString("status", "unknown");
		status.driverVersion = jsonStatus.optString("driverVersion", "unknown");
		
		return status;
	}
}
