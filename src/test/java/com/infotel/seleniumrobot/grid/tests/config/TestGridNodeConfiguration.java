package com.infotel.seleniumrobot.grid.tests.config;

import java.util.Arrays;

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
				+ "detect-drivers = false");
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
				+ "foo = 3");
	}
	
	@Test(groups={"grid"})
	public void testToTomlWithDrivers() {
		
		MutableCapabilities browserCaps = new MutableCapabilities();
		
		browserCaps.setCapability("max-sessions", 3);
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
				+ "stereotype = \"{\\\"browserName\\\":\\\"firefox\\\",\\\"browserVersion\\\":\\\"100.0\\\",\\\"max-sessions\\\":3,\\\"platformName\\\":\\\"WIN10\\\",\\\"restrictToTags\\\":false,\\\"sr:beta\\\":false,\\\"sr:nodeTags\\\":\\\"foo\\\",\\\"webdriver-executable\\\":\\\"geckodriver.exe\\\"}\"");
	}
}
