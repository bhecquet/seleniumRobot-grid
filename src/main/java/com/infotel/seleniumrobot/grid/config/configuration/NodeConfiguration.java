package com.infotel.seleniumrobot.grid.config.configuration;

import java.io.File;
import java.util.HashMap;

import org.json.JSONObject;

public class NodeConfiguration extends HashMap<String, Object> {
	private static final String DEFAULT_PROXY = "com.infotel.seleniumrobot.grid.CustomRemoteProxy";
	private static final Integer DEFAULT_MAX_SESSION = 10;
	private static final Integer DEFAULT_PORT = 5555;
	private static final String DEFAULT_HOST = "ip";
	private static final Boolean DEFAULT_REGISTER = true;
	private static final Integer DEFAULT_REGISTER_CYCLE = 5000;
	private static final Integer DEFAULT_HUB_PORT = 4444;
	private static final String DEFAULT_HUB_HOST = "ip";
	private static final String[] DEFAULT_SERVLETS = new String[] {"com.infotel.seleniumrobot.grid.servlets.server.MobileNodeServlet",
											"com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet"};
	
	private static final String PROXY = "proxy";
	private static final String MAX_SESSIONS = "maxSessions";
	private static final String PORT = "port";
	private static final String HOST = "host";
	private static final String REGISTER = "register";
	private static final String REGISTER_CYCLE = "registerCycle";
	private static final String HUB_PORT = "hubPort";
	private static final String HUB_HOST = "hubHost";
	private static final String SERVLETS = "servlets";
	
	public NodeConfiguration() {
		setProxy(DEFAULT_PROXY);
		setMaxSession(DEFAULT_MAX_SESSION);
		setPort(DEFAULT_PORT);
		setHost(DEFAULT_HOST);
		setRegister(DEFAULT_REGISTER);
		setRegisterCycle(DEFAULT_REGISTER_CYCLE);
		setHubHost(DEFAULT_HUB_HOST);
		setHubPort(DEFAULT_HUB_PORT);
		setServlets(DEFAULT_SERVLETS);
		
	}
	
	public String toJson() {
		return new JSONObject(this).toString(4);
	}
	
	public static NodeConfiguration fromJson(JSONObject jsonNode) {
		NodeConfiguration conf = new NodeConfiguration();
		conf.putAll(jsonNode.toMap());
		return conf;
	}
	
	public Integer getMaxSession() {
		return (Integer)get(MAX_SESSIONS);
	}
	public void setMaxSession(int maxSession) {
		put(MAX_SESSIONS, maxSession);
	}
	public Integer getPort() {
		return (Integer)get(PORT);
	}
	public void setPort(int port) {
		put(PORT, port);
	}
	public String getHost() {
		return (String)get(HOST);
	}
	public void setHost(String ip) {
		put(HOST, ip);
	}
	public boolean isRegister() {
		return (Boolean)get(REGISTER);
	}
	public void setRegister(boolean register) {
		put(REGISTER, register);
	}
	public Integer getRegisterCycle() {
		return (Integer)get(REGISTER_CYCLE);
	}
	public void setRegisterCycle(int registerCycle) {
		put(REGISTER_CYCLE, registerCycle);
	}
	public Integer getHubPort() {
		return (Integer)get(HUB_PORT);
	}
	public void setHubPort(int hubPort) {
		put(HUB_PORT, hubPort);
	}
	public String getHubHost() {
		return (String)get(HUB_HOST);
	}
	public void setHubHost(String hubHost) {
		put(HUB_HOST, hubHost);
	}
	public String getProxy() {
		return (String)get(PROXY);
	}
	private void setProxy(String hubHost) {
		put(PROXY, hubHost);
	}
	public String[] getServlets() {
		return (String[])get(SERVLETS);
	}
	private void setServlets(String[] servlets) {
		put(SERVLETS, servlets);
	}
}
