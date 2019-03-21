/**
 * Copyright 2017 www.infotel.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infotel.seleniumrobot.grid.tests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.CustomCapabilityMatcher;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

import io.appium.java_client.remote.MobileCapabilityType;

public class TestCustomCapabilityMatcher {

	/**
	 * Test when all capabilities match
	 */
	@Test(groups={"grid"})
	public void testDesktopBrowserMatching() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when all mobile capabilities match
	 */
	@Test(groups={"grid"})
	public void testMobileBrowserMatching() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCapability.put(MobileCapabilityType.PLATFORM_VERSION, "5.0");
		nodeCapability.put(MobileCapabilityType.BROWSER_NAME, "chrome");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		requestedCapability.put(MobileCapabilityType.PLATFORM_VERSION, "5.0");
		requestedCapability.put(MobileCapabilityType.BROWSER_NAME, "chrome");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when a capability does not match
	 */
	@Test(groups={"grid"})
	public void testMobileBrowserNotMatching() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCapability.put(MobileCapabilityType.PLATFORM_VERSION, "6.0");
		nodeCapability.put(MobileCapabilityType.BROWSER_NAME, "chrome");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		requestedCapability.put(MobileCapabilityType.PLATFORM_VERSION, "5.0");
		requestedCapability.put(MobileCapabilityType.BROWSER_NAME, "chrome");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when that mobile node does not match desktop capabilities
	 */
	@Test(groups={"grid"})
	public void testDesktopBrowserNotMatchingMobileNode() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCapability.put(MobileCapabilityType.PLATFORM_VERSION, "6.0");
		nodeCapability.put(MobileCapabilityType.BROWSER_NAME, "chrome");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.PLATFORM, "ANY");
		requestedCapability.put(CapabilityType.VERSION, "");
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}

	
	/**
	 * Test when a required capability (browser version) is not in node caps
	 * No match expected
	 */
	@Test(groups={"grid"})
	public void testMissingRequiredCapability() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(CapabilityType.VERSION, "45");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when capability is present is node but not required
	 */
	@Test(groups={"grid"})
	public void testMoreCapabilitiesOnNode() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(CapabilityType.VERSION, "45");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test with value any, which matches anything
	 */
	@Test(groups={"grid"})
	public void testAnyValue() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "ANY");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when capability is present is node but not required
	 */
	@Test(groups={"grid"})
	public void testWrongPlatform() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WONDOWS");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when we request an OS family and node specified a member of this family (requested: Windows, node: Vista)
	 */
	@Test(groups={"grid"})
	public void testPlatformFamily() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when we request an OS family and node specified a member of this family (requested: Windows, node: Vista)
	 */
	@Test(groups={"grid"})
	public void testPlatformExact() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when we request an OS family and node specified a member of this family (requested: Windows, node: Vista)
	 */
	@Test(groups={"grid"})
	public void testPlatformNotExact() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "XP");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}

	/**
	 * Test when capability is present is node but not required
	 */
	@Test(groups={"grid"})
	public void testSeveralBrowsersOnNodeOneMatches() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome,browser");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when capability is present is node but not required
	 */
	@Test(groups={"grid"})
	public void testSeveralBrowsersOnNodeNoneMatches() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome,browser");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "firefox");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test that when comparing mobile capabilities, platform key is ignored if platformName is present
	 */
	@Test(groups={"grid"})
	public void testPlatformNameWithPlatform() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome,browser");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "browser");
		requestedCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		requestedCapability.put(CapabilityType.PLATFORM, "android");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test that when comparing mobile capabilities, version key is ignored if platformVersion is present
	 */
	@Test(groups={"grid"})
	public void testPlatformNameWithVersion() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCapability.put(MobileCapabilityType.PLATFORM_VERSION, "6.0");
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome,browser");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(MobileCapabilityType.PLATFORM_NAME, "android");
		requestedCapability.put(CapabilityType.BROWSER_NAME, "browser");
		requestedCapability.put(MobileCapabilityType.PLATFORM_VERSION, "6.0");
		requestedCapability.put(CapabilityType.VERSION, "6.0");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client does not request particular tag and this tag is not set
	 * Matching is true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsNotRequestedNotSet() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client does request particular tag and this tag is not set on nodes
	 * Matching is false because tag matching is mandatory
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedNotSet() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("foo"));
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client does not request particular tag and this tag is set
	 * Matching is true because then, node tags are ignored
	 */
	@Test(groups={"grid"})
	public void testNodeTagsNotRequestedSet() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("foo"));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client does request particular tag and this tag is set. requested and set tag match 
	 * Matching is true 
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndMatching() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("foo"));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("foo"));
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client does request particular tag and this tag is not set. requested and set tag do not match 
	 * Matching is false
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndNotMatching() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("foo"));
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client does request particular tag and this tag is set. requested and set tag do not fully match, only 1 tag matches 
	 * Matching is false
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndPartialMatching() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("foo", "bar"));
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client does request particular tag and this tag is set. requested and set tag match one one of node tags
	 * Matching is false
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndMatchingOnOneTag() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("foo", "bar"));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Requested node tag is a string instead of a list, it's ignored and matching is done
	 * Matching is true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedAsString() {
		new LaunchConfig(new String[] {"-role", "node"});
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, "bar");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	
	/**
	 * Test when client does not request particular tag. Node declares a tag and restrictToTags=true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRestricted() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
		new LaunchConfig(new String[] {"-role", "node", "-restrictToTags", "true"});
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test when client requests particular tag. Node declares a tag and restrictToTags=true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRestrictedWithMatchingTag() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "VISTA");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
		LaunchConfig config = new LaunchConfig(new String[] {"-role", "node", "-restrictToTags", "true"});
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, "bar");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}

}
