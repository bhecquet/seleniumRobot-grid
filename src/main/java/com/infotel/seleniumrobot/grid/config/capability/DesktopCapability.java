package com.infotel.seleniumrobot.grid.config.capability;

public class DesktopCapability extends NodeCapability {

	protected static final String PLATFORM = "platform";

	public String getPlatform() {
		return (String)get(PLATFORM);
	}

	public void setPlatform(String platform) {
		put(PLATFORM, platform);
	}
}
