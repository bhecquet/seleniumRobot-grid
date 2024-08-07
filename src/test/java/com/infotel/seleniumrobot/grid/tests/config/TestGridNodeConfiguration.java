package com.infotel.seleniumrobot.grid.tests.config;

import java.util.Arrays;

import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

public class TestGridNodeConfiguration {

	@Test(groups={"grid"})
	public void testToToml() {
		String toml = new GridNodeConfiguration().toToml();
		Assert.assertEquals(toml.trim().replace("\r", ""), "[node]\n"
				+ "detect-drivers = false\n" +
				"\n" +
				"\n" +
				"\n" +
				"[distributor]\n" +
				"slot-matcher = \"com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher\"");
	}
	
	/**
	 * Check String and int parameters are correctly handled
	 */
	@Test(groups={"grid"})
	public void testToTomlWithConfiguration() {
		GridNodeConfiguration nodeConfig = new GridNodeConfiguration();
		nodeConfig.addNodeConfiguration("foo", 3);
		nodeConfig.addNodeConfiguration("bar", "value");
		String toml = nodeConfig.toToml();
		Assert.assertEquals(toml.trim().replace("\r", ""), "[node]\n"
				+ "bar = \"value\"\n"
				+ "detect-drivers = false\n"
				+ "foo = 3\n" +
				"\n" +
				"\n" +
				"\n" +
				"[distributor]\n" +
				"slot-matcher = \"com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher\"");
	}
	
	@Test(groups={"grid"})
	public void testToTomlWithDrivers() {
		
		MutableCapabilities browserCaps = new MutableCapabilities();
		
		browserCaps.setCapability(LaunchConfig.TOTAL_SESSIONS, 3);
		browserCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, "foo");
		browserCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, false);
		browserCaps.setCapability(GridNodeConfiguration.WEBDRIVER_PATH, "geckodriver.exe");
		browserCaps.setCapability(CapabilityType.BROWSER_NAME, "firefox");
		browserCaps.setCapability(CapabilityType.PLATFORM_NAME, "Windows 10");
		browserCaps.setCapability(CapabilityType.BROWSER_VERSION, "100.0");
		browserCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, false);
		
		GridNodeConfiguration nodeConfig = new GridNodeConfiguration();
		nodeConfig.capabilities = Arrays.asList(browserCaps);
		String toml = nodeConfig.toToml();
		Assert.assertEquals(toml.trim().replace("\r", ""), "[node]\n"
				+ "detect-drivers = false\n"
				+ "\n"
				+ "\n"
				+ "[[node.driver-configuration]]\n"
				+ "display-name = \"firefox 100.0\"\n"
				+ "webdriver-executable = \"geckodriver.exe\"\n"
				+ "max-sessions = 3\n"
				+ "stereotype = \"{\\\"sr:nodeTags\\\":\\\"foo\\\",\\\"browserVersion\\\":\\\"100.0\\\",\\\"browserName\\\":\\\"firefox\\\",\\\"platformName\\\":\\\"WIN10\\\",\\\"sr:restrictToTags\\\":false,\\\"sr:beta\\\":false}\"\n" +
				"\n" +
				"\n" +
				"[distributor]\n" +
				"slot-matcher = \"com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher\"");
	}
	

	@Test(groups={"grid"})
	public void testToTomlWithMobile() {

		XCUITestOptions mobileCaps = new XCUITestOptions();
		
		mobileCaps.setCapability(LaunchConfig.TOTAL_SESSIONS, 3);
		mobileCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, "foo");
		mobileCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, false);
		mobileCaps.setCapability(CapabilityType.BROWSER_NAME, "SAFARI v0.0");
		mobileCaps.setCapability(CapabilityType.BROWSER_VERSION, "100.0");
		mobileCaps.setCapability(CapabilityType.PLATFORM_NAME, "iOS");
		mobileCaps.setPlatformVersion("16.2");
		mobileCaps.setDeviceName("iPhone 14");
		mobileCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, false);
		
		GridNodeConfiguration nodeConfig = new GridNodeConfiguration();
		nodeConfig.appiumUrl = "http://localhost:10000/wd/hub";
		nodeConfig.mobileCapabilities = Arrays.asList(mobileCaps);
		String toml = nodeConfig.toToml();
		Assert.assertEquals(toml.trim().replace("\r", ""), "[node]\n"
				+ "detect-drivers = false\n"
				+ "\n"
				+ "\n"
				+ "\n"
				+ "[distributor]\n"
				+ "slot-matcher = \"com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher\"\n"
				+ "[relay]\n"
				+ "url = \"http://localhost:10000/wd/hub\"\n"
				+ "status-endpoint = \"/status\"\n"
				+ "configs = [\"1\",\"{\\\"sr:nodeTags\\\":\\\"foo\\\",\\\"browserVersion\\\":\\\"100.0\\\",\\\"browserName\\\":\\\"SAFARI v0.0\\\",\\\"appium:deviceName\\\":\\\"iPhone 14\\\",\\\"platformName\\\":\\\"IOS\\\",\\\"appium:platformVersion\\\":\\\"16.2\\\",\\\"sr:restrictToTags\\\":false,\\\"sr:beta\\\":false}\"]");
	}
	
	@Test(groups={"grid"})
	public void testToTomlWithSafariBrowser() {
		
		MutableCapabilities browserCaps = new MutableCapabilities();
		
		browserCaps.setCapability(LaunchConfig.TOTAL_SESSIONS, 3);
		browserCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, "foo");
		browserCaps.setCapability(LaunchConfig.RESTRICT_TO_TAGS, false);
		browserCaps.setCapability(CapabilityType.BROWSER_NAME, "safari");
		browserCaps.setCapability(CapabilityType.PLATFORM_NAME, "MAC");
		browserCaps.setCapability(CapabilityType.BROWSER_VERSION, "100.0");
		browserCaps.setCapability(SeleniumRobotCapabilityType.BETA_BROWSER, false);
		
		GridNodeConfiguration nodeConfig = new GridNodeConfiguration();
		nodeConfig.capabilities = Arrays.asList(browserCaps);
		String toml = nodeConfig.toToml();
		Assert.assertEquals(toml.trim().replace("\r", ""), "[node]\n"
				+ "detect-drivers = false\n"
				+ "\n"
				+ "\n"
				+ "[[node.driver-configuration]]\n"
				+ "display-name = \"safari 100.0\"\n"
				+ "max-sessions = 3\n"
				+ "stereotype = \"{\\\"sr:nodeTags\\\":\\\"foo\\\",\\\"browserVersion\\\":\\\"100.0\\\",\\\"browserName\\\":\\\"safari\\\",\\\"platformName\\\":\\\"MAC\\\",\\\"sr:restrictToTags\\\":false,\\\"sr:beta\\\":false}\"\n" +
				"\n" +
				"\n" +
				"[distributor]\n" +
				"slot-matcher = \"com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher\"");
	}
}
