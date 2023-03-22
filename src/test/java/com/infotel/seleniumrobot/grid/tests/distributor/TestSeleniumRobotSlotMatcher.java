package com.infotel.seleniumrobot.grid.tests.distributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.aspects.SessionSlotActions;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

import io.appium.java_client.remote.MobileCapabilityType;

public class TestSeleniumRobotSlotMatcher {

	/**
	 * Test when all capabilities match
	 */
	@Test(groups={"grid"})
	public void testDesktopBrowserMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
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
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
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
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
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
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "ANY");
		requestedCapability.put(CapabilityType.BROWSER_VERSION, "");
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}

	
	/**
	 * Test when a required capability (browser version) is not in node caps
	 * No match expected
	 */
	@Test(groups={"grid"})
	public void testMissingRequiredCapability() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(CapabilityType.BROWSER_VERSION, "45");
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when capability is present is node but not required
	 */
	@Test(groups={"grid"})
	public void testMoreCapabilitiesOnNode() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(CapabilityType.BROWSER_VERSION, "45");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test with value any, which matches anything
	 */
	@Test(groups={"grid"})
	public void testAnyValue() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "ANY");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when we request an OS family and node specified a member of this family (requested: Windows, node: Vista)
	 */
	@Test(groups={"grid"})
	public void testPlatformFamily() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when we request an OS family and node specified a member of this family (requested: Windows, node: Vista)
	 */
	@Test(groups={"grid"})
	public void testPlatformExact() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when we request an OS family and node specified a member of this family (requested: Windows, node: Vista)
	 */
	@Test(groups={"grid"})
	public void testPlatformNotExact() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "XP");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}

	/**
	 * Test when capability is present is node but not required
	 */
	@Test(groups={"grid"})
	public void testSeveralBrowsersOnNodeOneMatches() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome,browser");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when capability is present is node but not required
	 */
	@Test(groups={"grid"})
	public void testSeveralBrowsersOnNodeNoneMatches() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome,browser");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "firefox");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
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
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "android");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
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
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client does not request particular tag and this tag is not set
	 * Matching is true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsNotRequestedNotSet() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client does request particular tag and this tag is not set on nodes
	 * Matching is false because tag matching is mandatory
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedNotSet() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("foo")));
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client does not request particular tag and this tag is set
	 * Matching is true because then, node tags are ignored
	 */
	@Test(groups={"grid"})
	public void testNodeTagsNotRequestedSet() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("foo")));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client does request particular tag and this tag is set. requested and set tag match 
	 * Matching is true 
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("foo")));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("foo")));
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client does request particular tag and this tag is not set. requested and set tag do not match 
	 * Matching is false
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndNotMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("bar")));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("foo")));
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client does request particular tag and this tag is set. requested and set tag do not fully match, only 1 tag matches 
	 * Matching is false
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndPartialMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("bar")));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("foo", "bar")));
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client does request particular tag and this tag is set. requested and set tag match one of node tags
	 * Matching is true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRequestedSetAndMatchingOnOneTag() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("foo", "bar")));
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("bar")));
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	
	/**
	 * Test when client does not request particular tag. Node declares a tag and restrictToTags=true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRestricted() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("bar")));
		nodeCapability.put(LaunchConfig.RESTRICT_TO_TAGS, true);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test when client requests particular tag. Node declares a tag and restrictToTags=true
	 */
	@Test(groups={"grid"})
	public void testNodeTagsRestrictedWithMatchingTag() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		nodeCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("bar")));
		nodeCapability.put(LaunchConfig.RESTRICT_TO_TAGS, true);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "Windows 10");
		requestedCapability.put(SeleniumRobotCapabilityType.NODE_TAGS, new ArrayList<>(Arrays.asList("bar")));
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new ImmutableCapabilities(nodeCapability), new ImmutableCapabilities(requestedCapability)));
	}
	
	/**
	 * When beta browser capability is requested, and provided
	 */
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * When beta browser capability is not requested but provided
	 */
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserNotMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, false);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Node capability for beta browser not set, but requested
	 * In this case, 'beta' capability is not taken into account
	 */
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserNotProvided() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	@Test(groups={"grid"})
	public void testDesktopBetaBrowserProvidedNotRequested() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(SeleniumRobotCapabilityType.BETA_BROWSER, true);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test the case where IE is there, Edge also, and we request Edge in IE mode
	 */
	@Test(groups={"grid"})
	public void testDesktopEdgeIeMode() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(SessionSlotActions.EDGE_PATH, "C:\\msedge.exe");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, true);
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test the case where IE is there, Edge also, and we request IE
	 */
	@Test(groups={"grid"})
	public void testDesktopIe() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(SessionSlotActions.EDGE_PATH, "C:\\msedge.exe");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, false);
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test the case where IE is there, not Edge and we request Edge in IE mode
	 */
	@Test(groups={"grid"})
	public void testDesktopEdgeIeModeNotAvailable() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(SessionSlotActions.EDGE_PATH, null);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, true);
		
		Assert.assertFalse(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
	
	/**
	 * Test the case where IE is there, not Edge and we request IRE
	 */
	@Test(groups={"grid"})
	public void testDesktopIeModeEdgeNotAvailable() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		nodeCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		nodeCapability.put(SessionSlotActions.EDGE_PATH, null);
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, Browser.IE.browserName());
		requestedCapability.put(CapabilityType.PLATFORM_NAME, "WINDOWS");
		requestedCapability.put(SeleniumRobotCapabilityType.EDGE_IE_MODE, false);
		
		Assert.assertTrue(new SeleniumRobotSlotMatcher().matches(new MutableCapabilities(nodeCapability), new MutableCapabilities(requestedCapability)));
	}
}
