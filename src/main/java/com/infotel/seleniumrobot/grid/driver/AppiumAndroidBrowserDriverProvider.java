package com.infotel.seleniumrobot.grid.driver;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DriverProvider;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;

public class AppiumAndroidBrowserDriverProvider extends AppiumDriverProvider implements DriverProvider {

	@Override
	public Capabilities getProvidedCapabilities() {
		
		DesiredCapabilities androidCaps = new DesiredCapabilities();
		androidCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
		androidCaps.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		androidCaps.setCapability(CapabilityType.BROWSER_NAME, "browser");
		
		return androidCaps;
	}

	@Override
	public boolean canCreateDriverInstanceFor(Capabilities capabilities) {
	    return "browser".equals(capabilities.getBrowserName()) && "android".equals(capabilities.getCapability(MobileCapabilityType.PLATFORM_NAME));
	}

	@Override
	protected Class<? extends WebDriver> getDriverClass() {
		return AndroidDriver.class;
	}

}
