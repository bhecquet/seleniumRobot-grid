package com.infotel.seleniumrobot.grid.driver;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DriverProvider;

import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;

public class AppiumSafariBrowserDriverProvider extends AppiumDriverProvider implements DriverProvider {


	@Override
	public Capabilities getProvidedCapabilities() {

		DesiredCapabilities iosCaps = new DesiredCapabilities();
		iosCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "ios");
		iosCaps.setCapability(CapabilityType.BROWSER_NAME, "safari");
		iosCaps.setCapability(CapabilityType.PLATFORM, Platform.MAC);

		return iosCaps;
	}

	@Override
	public boolean canCreateDriverInstanceFor(Capabilities capabilities) {
	    return "safari".equals(capabilities.getBrowserName()) && "ios".equals(capabilities.getCapability(MobileCapabilityType.PLATFORM_NAME));
	}

	@Override
	protected Class<? extends WebDriver> getDriverClass() {
		return IOSDriver.class;
	}

}
