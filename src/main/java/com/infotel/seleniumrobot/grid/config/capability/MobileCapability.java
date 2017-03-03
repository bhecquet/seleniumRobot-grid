package com.infotel.seleniumrobot.grid.config.capability;

public class MobileCapability extends NodeCapability {

	protected static final String PLATFORM_NAME = "platformName";
	protected static final String PLATFORM_VERSION = "platformVersion";
	protected static final String DEVICE_NAME = "deviceName";
	
	public MobileCapability() {
		super();
		setMaxInstances(1);
	}
	
	public String getPlatformName() {
		return (String)get(PLATFORM_NAME);
	}
	public void setPlatformName(String platformName) {
		put(PLATFORM_NAME, platformName);
	}
	public String getPlatformVersion() {
		return (String)get(PLATFORM_VERSION);
	}
	public void setPlatformVersion(String platformVersion) {
		put(PLATFORM_VERSION, platformVersion);
	}
	public String getDeviceName() {
		return (String)get(DEVICE_NAME);
	}
	public void setDeviceName(String deviceName) {
		put(DEVICE_NAME, deviceName);
	}
}
