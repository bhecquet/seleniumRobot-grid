package com.infotel.seleniumrobot.grid.tests.config;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.capability.DesktopCapability;
import com.infotel.seleniumrobot.grid.config.capability.MobileCapability;
import com.infotel.seleniumrobot.grid.config.capability.NodeCapability;
import com.seleniumtests.customexception.ConfigurationException;

public class TestNodeCapability {

	@Test(groups={"grid"})
	public void testToJson() {
		DesktopCapability caps = new DesktopCapability();
		caps.setBrowserName("firefox");
		caps.setPlatform("windows");

		String jsonString = caps.toJson();
		Assert.assertTrue(jsonString.contains("\"browserName\": \"firefox\""));
		Assert.assertTrue(jsonString.contains("\"platform\": \"windows\""));
		Assert.assertTrue(jsonString.contains("\"maxInstances\": 5"));
		Assert.assertTrue(jsonString.contains("\"seleniumProtocol\": \"WebDriver\""));
	}
	
	@Test(groups={"grid"})
	public void fromJson() {
		String json = "{" +
          "\"browserName\": \"chrome\"," +
          "\"platformName\": \"android\"," +
          "\"deviceName\": \"wiko\"," +
          "\"platformVersion\": \"6.0\"," +
          "\"seleniumProtocol\": \"WebDriver\"" +
          "}";
		
		MobileCapability conf = (MobileCapability)NodeCapability.fromJson(new JSONObject(json));
		Assert.assertEquals(conf.getPlatformName(), "android");
		Assert.assertEquals(conf.getDeviceName(), "wiko");
	}
	
	@Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
	public void fromJsonNotFound() {
		String json = "{" +
				"\"browserName\": \"chrome\"," +
				"\"deviceName\": \"wiko\"," +
				"\"seleniumProtocol\": \"WebDriver\"" +
				"}";
		
		NodeCapability.fromJson(new JSONObject(json));
	}
}
