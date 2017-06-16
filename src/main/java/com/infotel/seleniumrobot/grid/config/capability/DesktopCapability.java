package com.infotel.seleniumrobot.grid.config.capability;

public class DesktopCapability extends NodeCapability {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final String PLATFORM = "platform";

	public String getPlatform() {
		return (String)get(PLATFORM);
	}

	public void setPlatform(String platform) {
		put(PLATFORM, platform);
	}
}
