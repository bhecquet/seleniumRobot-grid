package com.infotel.seleniumrobot.grid.tests;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.CustomCapabilityMatcher;

import io.appium.java_client.remote.MobileCapabilityType;

public class TestCustomCapabilityMatcher {

	/**
	 * Test when all capabilities match
	 */
	@Test(groups={"grid"})
	public void testDesktopBrowserMatching() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "windows");
		
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
	 * Test when a required capability (browser version) is not in node caps
	 * No match expected
	 */
	@Test(groups={"grid"})
	public void testMissingRequiredCapability() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "windows");
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
		nodeCapability.put(CapabilityType.PLATFORM, "windows");
		nodeCapability.put(CapabilityType.VERSION, "45");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "windows");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	/**
	 * Test with value any, which matches anything
	 */
	@Test(groups={"grid"})
	public void testAnyValue() {
		Map<String, Object> nodeCapability = new HashMap<>();
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		nodeCapability.put(CapabilityType.PLATFORM, "windows");
		
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
		nodeCapability.put(CapabilityType.PLATFORM, "windows");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "chrome");
		requestedCapability.put(CapabilityType.PLATFORM, "wondows");
		
		Assert.assertFalse(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
}
