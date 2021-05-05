package com.infotel.seleniumrobot.grid.tests.config;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.util.osutility.OSUtility;

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
	
	/**
	 * -maxNodeTestCount option is unknown for selenium grid and so, it must be removed from args before being passed to grid server
	 */
	@Test(groups={"grid"})
	public void testMaxNodeTestCountRemovedFromArgs() {
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "hub", "-hubConfig", "conf.json", "-maxNodeTestCount", "10"});
		Assert.assertEquals(config.getMaxNodeTestCount(), (Integer)10);
		Assert.assertFalse(config.getArgList().contains("-maxNodeTestCount"));
		Assert.assertFalse(config.getArgList().contains("10"));
	}
	
	/**
	 * -maxHubTestCount option is unknown for selenium grid and so, it must be removed from args before being passed to grid server
	 */
	@Test(groups={"grid"})
	public void testMaxHubTestCountRemovedFromArgs() {
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "hub", "-hubConfig", "conf.json", "-maxHubTestCount", "10"});
		Assert.assertEquals(config.getMaxHubTestCount(), (Integer)10);
		Assert.assertFalse(config.getArgList().contains("-maxHubTestCount"));
		Assert.assertFalse(config.getArgList().contains("10"));
	}
	
	/**
	 * Check that by default, maxHubTestCount and maxNodeTestCount are set to null
	 */
	@Test(groups={"grid"})
	public void testTestCountDefaultedToNull() {
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "hub", "-hubConfig", "conf.json"});
		Assert.assertNull(config.getMaxHubTestCount());
		Assert.assertNull(config.getMaxNodeTestCount());
	}
	
	/**
	 * -nodeTags option is unknown for selenium grid and so, it must be removed from args before being passed to grid server
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRemovedFromArgs() {
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "node", "-nodeTags", "foo"});
		Assert.assertEquals(config.getNodeTags().get(0), "foo");
		Assert.assertFalse(config.getArgList().contains("-nodeTags"));
		Assert.assertFalse(config.getArgList().contains("foo"));
	}
	
	@Test(groups={"grid"})
	public void testNodeTags() {
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "node", "-nodeTags", "foo, bar"});
		Assert.assertTrue(config.getNodeTags().size() > 0);
		Assert.assertEquals(config.getNodeTags().get(0), "foo");
		Assert.assertEquals(config.getNodeTags().get(1), "bar");
	}
	
	@Test(groups={"grid"})
	public void testNoExternalPrograms() { 
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "node", "-nodeTags", "foo, bar"});
		if (OSUtility.isWindows()) {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 2);
		} else {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 1);
		}
	}
	
	@Test(groups={"grid"})
	public void testExternalProgramsOption() { 
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "node", "-extProgramWhiteList", "foo, bar"});
		if (OSUtility.isWindows()) {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 4);
		} else {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 3);
		}
		Assert.assertTrue(config.getExternalProgramWhiteList().contains("foo"));
		Assert.assertTrue(config.getExternalProgramWhiteList().contains("bar"));
	}
}
