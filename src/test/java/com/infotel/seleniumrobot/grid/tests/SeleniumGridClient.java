package com.infotel.seleniumrobot.grid.tests;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.appium.java_client.remote.MobileCapabilityType;

public class SeleniumGridClient {

	public static void main(String [] args) throws MalformedURLException {
		DesiredCapabilities capability = new DesiredCapabilities();
		capability.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capability.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
//		capability.setPlatform(Platform.VISTA);
		capability.setCapability(MobileCapabilityType.DEVICE_NAME, "Google Nexus 6 - 6.0.0 - API 23 - 1440x256");
		WebDriver driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capability);
	}
}
