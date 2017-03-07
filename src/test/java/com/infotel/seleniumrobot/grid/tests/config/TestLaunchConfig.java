package com.infotel.seleniumrobot.grid.tests.config;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.customexception.ConfigurationException;

public class TestLaunchConfig {

	@Test(groups={"grid"})
	public void testNodeRole() throws IOException {
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "node", "-nodeConfig", "conf.json", "-browser", "browserName=firefox,version=3.6"});
		Assert.assertFalse(config.getHubRole());
		Assert.assertEquals(config.getConfigPath(), "conf.json");
		Assert.assertEquals(config.getBrowserConfig().size(), 1);
	}
	
	@Test(groups={"grid"})
	public void testHubRole() throws IOException {
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "hub", "-hubConfig", "conf.json"});
		Assert.assertTrue(config.getHubRole());
		Assert.assertEquals(config.getConfigPath(), "conf.json");
		Assert.assertEquals(config.getBrowserConfig().size(), 0);
	}
	
	@Test(groups={"grid"}, expectedExceptions=ConfigurationException.class)
	public void testNoRole() throws IOException {
		new LaunchConfig(new String[] {"-hubConfig", "conf.json"});
	}
}
