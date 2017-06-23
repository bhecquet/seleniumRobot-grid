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
		nodeCapability.put(MobileCapabilityType.PLATFORM_VERSION, "6.0");
		nodeCapability.put(CapabilityType.BROWSER_NAME, "chrome,browser");
		
		Map<String, Object> requestedCapability = new HashMap<>();
		requestedCapability.put(CapabilityType.BROWSER_NAME, "browser");
		requestedCapability.put(MobileCapabilityType.PLATFORM_VERSION, "6.0");
		requestedCapability.put(CapabilityType.VERSION, "6.0");
		
		Assert.assertTrue(new CustomCapabilityMatcher().matches(nodeCapability, requestedCapability));
	}
	
	
}
