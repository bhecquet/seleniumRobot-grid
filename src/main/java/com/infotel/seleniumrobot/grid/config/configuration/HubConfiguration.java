package com.infotel.seleniumrobot.grid.config.configuration;

import java.io.File;
import java.util.HashMap;

import org.json.JSONObject;

public class HubConfiguration extends HashMap<String, Object> {
	private static final String DEFAULT_PROXY = "com.infotel.seleniumrobot.grid.CustomRemoteProxy";
	private static final Integer DEFAULT_MAX_SESSION = 10;
	private static final Integer DEFAULT_PORT = 4444;
	private static final String DEFAULT_HOST = null;
	private static final String DEFAULT_PRIORITIZER = null;
	private static final String DEFAULT_CAPABILITY_MATCHER = "com.infotel.seleniumrobot.grid.CustomCapabilityMatcher";
	private static final Integer DEFAULT_NEW_SESSION_WAIT_TIMEOUT = -1;
	private static final Integer DEFAULT_NODE_POLLING = 5000;
	private static final Integer DEFAULT_CLEANUP_CYCLE = 5000;
	private static final Integer DEFAULT_TIMEOUT = 300000;
	private static final Integer DEFAULT_BROWSER_TIMEOUT = 0;
	private static final Integer DEFAULT_JETTY_MAX_THREADS = -1;
	private static final Boolean DEFAULT_THROW_ON_CAPABILITY_NOT_PRESENT = true;
	private static final String DEFAULT_SERVLETS = "";

	private static final String NEW_SESSION_WAIT_TIMEOUT =  "newSessionWaitTimeout";
	private static final String PRIORITIZER = "prioritizer";
	private static final String CAPABILITY_MATCHER = "capabilityMatcher";
	private static final String THROW_ON_CAPABILITY_NOT_PRESENT = "throwOnCapabilityNotPresent";
	private static final String NODE_POLLING = "nodePolling";
	private static final String CLEANUP_CYCLE = "cleanUpCycle";
	private static final String TIMEOUT = "timeout";
	private static final String BROWSER_TIMEOUT = "browserTimeout";
	private static final String JETTY_MAX_THREADS = "jettyMaxThreads";
	private static final String PROXY = "proxy";
	private static final String MAX_SESSION = "maxSession";
	private static final String PORT = "port";
	private static final String HOST = "host";
	private static final String SERVLETS = "servlets";
	
	public HubConfiguration() {
		setProxy(DEFAULT_PROXY);
		setMaxSession(DEFAULT_MAX_SESSION);
		setPort(DEFAULT_PORT);
		setHost(DEFAULT_HOST);
		setServlets(DEFAULT_SERVLETS);
		setPrioritizer(DEFAULT_PRIORITIZER);
		setCapabilityMatcher(DEFAULT_CAPABILITY_MATCHER);
		setNewSessionWaitTimeout(DEFAULT_NEW_SESSION_WAIT_TIMEOUT);
		setNodePolling(DEFAULT_NODE_POLLING);
		setCleanupCycle(DEFAULT_CLEANUP_CYCLE);
		setTimeout(DEFAULT_TIMEOUT);
		setBrowserTimeout(DEFAULT_BROWSER_TIMEOUT);
		setJettyMaxThread(DEFAULT_JETTY_MAX_THREADS);
		setThrowOnCapabilityNotPresent(DEFAULT_THROW_ON_CAPABILITY_NOT_PRESENT);
		
	}
	
	public String toJson() {
		return new JSONObject(this).toString(4);
	}
	
	public static HubConfiguration fromJson(JSONObject jsonNode) {
		HubConfiguration conf = new HubConfiguration();
		conf.putAll(jsonNode.toMap());
		return conf;
	}
	
	public Integer getMaxSession() {
		return (Integer)get(MAX_SESSION);
	}
	public void setMaxSession(int maxSession) {
		put(MAX_SESSION, maxSession);
	}
	public Integer getPort() {
		return (Integer)get(PORT);
	}
	public void setPort(int port) {
		put(PORT, port);
	}
	public Integer getNewSessionWaitTimeout() {
		return (Integer)get(NEW_SESSION_WAIT_TIMEOUT);
	}
	public void setNewSessionWaitTimeout(int timeout) {
		put(NEW_SESSION_WAIT_TIMEOUT, timeout);
	}
	public Integer getTimeout() {
		return (Integer)get(TIMEOUT);
	}
	public void setTimeout(int timeout) {
		put(TIMEOUT, timeout);
	}
	public Integer getBrowserTimeout() {
		return (Integer)get(BROWSER_TIMEOUT);
	}
	public void setBrowserTimeout(int timeout) {
		put(BROWSER_TIMEOUT, timeout);
	}
	public Integer getNodePolling() {
		return (Integer)get(NODE_POLLING);
	}
	public void setNodePolling(int polling) {
		put(NODE_POLLING, polling);
	}
	public Integer getCleanupCycle() {
		return (Integer)get(CLEANUP_CYCLE);
	}
	public void setCleanupCycle(int cycle) {
		put(CLEANUP_CYCLE, cycle);
	}
	public Integer getJettyMaxThread() {
		return (Integer)get(JETTY_MAX_THREADS);
	}
	public void setJettyMaxThread(int nb) {
		put(JETTY_MAX_THREADS, nb);
	}
	public String getPrioritizer() {
		return (String)get(PRIORITIZER);
	}
	public void setPrioritizer(String prioritizer) {
		put(PRIORITIZER, prioritizer);
	}
	public String getCapabilityMatcher() {
		return (String)get(CAPABILITY_MATCHER);
	}
	public void setCapabilityMatcher(String cap) {
		put(CAPABILITY_MATCHER, cap);
	}
	public String getHost() {
		return (String)get(HOST);
	}
	public void setHost(String ip) {
		put(HOST, ip);
	}
	public Boolean getThrowOnCapabilityNotPresent() {
		return (Boolean)get(THROW_ON_CAPABILITY_NOT_PRESENT);
	}
	public void setThrowOnCapabilityNotPresent(boolean thro) {
		put(THROW_ON_CAPABILITY_NOT_PRESENT, thro);
	}
	public String getProxy() {
		return (String)get(PROXY);
	}
	private void setProxy(String hubHost) {
		put(PROXY, hubHost);
	}
	public String getServlets() {
		return (String)get(SERVLETS);
	}
	private void setServlets(String servlets) {
		put(SERVLETS, servlets);
	}
}
