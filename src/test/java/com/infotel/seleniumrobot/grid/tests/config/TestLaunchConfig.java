package com.infotel.seleniumrobot.grid.tests.config;

import java.io.IOException;

import org.openqa.selenium.Proxy;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.config.LaunchConfig.Role;
import com.seleniumtests.util.osutility.OSUtility;

public class TestLaunchConfig {

	@Test(groups={"grid"})
	public void testNodeRole() throws IOException {
		LaunchConfig config = new LaunchConfig(new String[] {"node"});
		Assert.assertEquals(config.getRole(), Role.NODE);
		
		// default parameters
		Assert.assertTrue(config.getNodeTags().isEmpty());
		Assert.assertEquals(config.getProxyConfig(), new Proxy());
		Assert.assertFalse(config.getDevMode());
		Assert.assertFalse(config.getRestrictToTags());
		Assert.assertNull(config.getNodePort());
		Assert.assertNull(config.getMaxSessions());
		if (OSUtility.isWindows()) {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 3);
		} else {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 2);
		}
		
		Assert.assertTrue(config.getArgList().contains("--node-implementation"));
		Assert.assertTrue(config.getArgList().contains("com.infotel.seleniumrobot.grid.node.SeleniumRobotNodeFactory"));
	}
	
	@Test(groups={"grid"})
	public void testHubRole() throws IOException {
		LaunchConfig config = new LaunchConfig(new String[] {"hub"});
		Assert.assertEquals(config.getRole(), Role.HUB);
		
		Assert.assertTrue(config.getArgList().contains("--slot-matcher"));
		Assert.assertTrue(config.getArgList().contains("com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher"));
		
		Assert.assertEquals((Integer)config.getRouterPort(), (Integer)4444);
		Assert.assertEquals(config.getRouterHost(), "localhost");
	}
	
	@Test(groups={"grid"}, expectedExceptions=IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No/wrong role provided")
	public void testNoRole() throws IOException {
		new LaunchConfig(new String[] {});
	}
	
	@Test(groups={"grid"}, expectedExceptions=IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No/wrong role provided")
	public void testWrongRole() throws IOException {
		new LaunchConfig(new String[] {"foo"});
	}
	
	/**
	 * --nodeTags option is unknown for selenium grid and so, it must be removed from args before being passed to grid server
	 */	
	@Test(groups={"grid"})
	public void testNodeTags() {
		LaunchConfig config = new LaunchConfig(new String[] {"node", "--nodeTags", "foo,bar"});
		Assert.assertTrue(config.getNodeTags().size() > 0);
		Assert.assertEquals(config.getNodeTags().get(0), "foo");
		Assert.assertEquals(config.getNodeTags().get(1), "bar");
		Assert.assertFalse(config.getArgList().contains("--nodeTags"));
		Assert.assertFalse(config.getArgList().contains("foo"));
	}
	@Test(groups={"grid"})
	public void testRestrictToTags() {
		LaunchConfig config = new LaunchConfig(new String[] {"node", "--nodeTags", "foo,bar", "--restrictToTags", "true"});
		Assert.assertTrue(config.getRestrictToTags());
	}
	
	@Test(groups={"grid"})
	public void testProxyConfig() {
		LaunchConfig config = new LaunchConfig(new String[] {"node", "--proxyConfig", "auto"});
		Assert.assertTrue(config.getProxyConfig().isAutodetect());
	}
	
	@Test(groups={"grid"})
	public void testNodePort() {
		LaunchConfig config = new LaunchConfig(new String[] {"node", "--port", "4321"});
		Assert.assertEquals((Integer)config.getNodePort(), (Integer)4321);
	}
	
	@Test(groups={"grid"})
	public void testMaxSessionsLow() {
		LaunchConfig config = new LaunchConfig(new String[] {"node", "--max-sessions", "1"});
		Assert.assertTrue(config.getArgList().contains("--max-sessions")); 
		Assert.assertTrue(config.getArgList().contains("3")); // at least 3 sessions to allow to attach an existing browser
	}
	
	@Test(groups={"grid"})
	public void testMaxSessions() {
		LaunchConfig config = new LaunchConfig(new String[] {"node", "--max-sessions", "3"});
		Assert.assertTrue(config.getArgList().contains("--max-sessions")); 
		Assert.assertTrue(config.getArgList().contains("3")); // at least 3 sessions to allow to attach an existing browser
	}
	
	@Test(groups={"grid"})
	public void testExternalProgramsOption() { 
		LaunchConfig config = new LaunchConfig(new String[] {"node", "--extProgramWhiteList", "foo,bar"});
		if (OSUtility.isWindows()) {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 5);
		} else {
			Assert.assertEquals(config.getExternalProgramWhiteList().size(), 4);
		}
		Assert.assertTrue(config.getExternalProgramWhiteList().contains("foo"));
		Assert.assertTrue(config.getExternalProgramWhiteList().contains("bar"));
	}
	
	@Test(groups={"grid"})
	public void testRouterPort() {
		LaunchConfig config = new LaunchConfig(new String[] {"hub", "--port", "4321"});
		Assert.assertEquals((Integer)config.getRouterPort(), (Integer)4321);
	}
	
	@Test(groups={"grid"})
	public void testRouterHost() {
		LaunchConfig config = new LaunchConfig(new String[] {"hub", "--host", "myHost"});
		Assert.assertEquals(config.getRouterHost(), "myHost");
	}
}
