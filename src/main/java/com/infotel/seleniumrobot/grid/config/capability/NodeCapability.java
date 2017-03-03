package com.infotel.seleniumrobot.grid.config.capability;

import java.util.HashMap;

import org.json.JSONObject;

import com.seleniumtests.customexception.ConfigurationException;

public class NodeCapability extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;
	
	private static final String BROWSER_NAME = "browserName";
	private static final String MAX_INSTANCES = "maxInstances";
	private static final String SELENIUM_PROTOCOL = "seleniumProtocol";
	private static final String DEFAULT_SELENIUM_PROTOCOL = "WebDriver";
	private static final int DEFAULT_MAX_INSTANCES = 5;
	
	protected NodeCapability() {
		setSeleniumProtocol(DEFAULT_SELENIUM_PROTOCOL);
		setMaxInstances(DEFAULT_MAX_INSTANCES);
	}
	
	public String toJson() {
		return new JSONObject(this).toString(4);
	}
	
	public static NodeCapability fromJson(JSONObject jsonNode) {
		NodeCapability conf;
		if (jsonNode.opt(MobileCapability.PLATFORM_NAME) != null && jsonNode.opt(MobileCapability.DEVICE_NAME) != null && jsonNode.opt(MobileCapability.PLATFORM_VERSION) != null) {
			conf = new MobileCapability();
		} else if ((jsonNode.opt(DesktopCapability.PLATFORM) != null || jsonNode.opt(BROWSER_NAME) != null) 
				&& jsonNode.opt(MobileCapability.PLATFORM_NAME) == null 
				&& jsonNode.opt(MobileCapability.DEVICE_NAME) == null
				&& jsonNode.opt(MobileCapability.PLATFORM_VERSION) == null) {
			conf = new DesktopCapability();
		} else {
			throw new ConfigurationException("cannot recognize type of capability");
		}
		conf.putAll(jsonNode.toMap());
		return conf;
	}
	
	public String getBrowserName() {
		return (String)get(BROWSER_NAME);
	}
	public void setBrowserName(String browserName) {
		put(BROWSER_NAME, browserName);
	}
	public Integer getMaxInstances() {
		return (Integer)get(MAX_INSTANCES);
	}
	public void setMaxInstances(Integer maxInstances) {
		put(MAX_INSTANCES, maxInstances);
	}
	public String getSeleniumProtocol() {
		return (String)get(SELENIUM_PROTOCOL);
	}
	public void setSeleniumProtocol(String seleniumProtocol) {
		put(SELENIUM_PROTOCOL, seleniumProtocol);
	}
	
	
}
