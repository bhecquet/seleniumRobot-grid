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

import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.CustomCapabilityMatcher;
import com.infotel.seleniumrobot.grid.CustomRemoteProxy;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

import io.appium.java_client.remote.MobileCapabilityType;

public class TestCustomCapabilityMatcher {

	/**
	 * Test when all capabilities match
	 */
	@Test(groups={"grid"})
	public void testDesktopBrowserMatching() {
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
		nodeCapability.put(LaunchConfig.RESTRICT_TO_TAGS, true);
		
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
		nodeCapability.put(LaunchConfig.RESTRICT_TO_TAGS, true);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "VISTA");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * When beta browser capability is requested, and provided
	 */
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * When beta browser capability is not requested but provided
	 */
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserNotMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, false);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Node capability for beta browser not set, but requested
	 * In this case, 'beta' capability is not taken into account
	 */
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserNotProvided() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserProvidedNotRequested() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test the case where IE is there, Edge also, and we request Edge in IE mode
	 */
	@Test(groups={"grid"})
	public void testDesktopEdgeIeMode() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(CustomRemoteProxy.EDGE_PATH, "C:\\msedge.exe");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, true);
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test the case where IE is there, Edge also, and we request IE
	 */
	@Test(groups={"grid"})
	public void testDesktopIe() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(CustomRemoteProxy.EDGE_PATH, "C:\\msedge.exe");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, false);
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test the case where IE is there, not Edge and we request Edge in IE mode
	 */
	@Test(groups={"grid"})
	public void testDesktopEdgeIeModeNotAvailable() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(CustomRemoteProxy.EDGE_PATH, null);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, true);
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test the case where IE is there, not Edge and we request IRE
	 */
	@Test(groups={"grid"})
	public void testDesktopIeModeEdgeNotAvailable() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		nodeCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		nodeCapability.put(CustomRemoteProxy.EDGE_PATH, null);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, BrowserType.IE);
		requestedCapability.put(CapabilityType.PLATFORM, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, false);
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}

}
