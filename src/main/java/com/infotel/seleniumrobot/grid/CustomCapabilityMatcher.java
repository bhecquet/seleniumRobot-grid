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
package com.infotel.seleniumrobot.grid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.openqa.grid.internal.utils.DefaultCapabilityMatcher;
import org.openqa.selenium.remote.CapabilityType;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

import io.appium.java_client.remote.MobileCapabilityType;

/**
 * Default (naive) implementation of the capability matcher.
 * <p>
 * The default capability matcher will look at all the key from the request do
 * not start with _ and will try to find a node that has at least those
 * capabilities.
 */

public class CustomCapabilityMatcher extends DefaultCapabilityMatcher {

	private static final Logger logger = Logger.getLogger(CustomCapabilityMatcher.class.getName());
	
	public CustomCapabilityMatcher() { 
		super();
		addToConsider(MobileCapabilityType.PLATFORM_VERSION);
		addToConsider(MobileCapabilityType.DEVICE_NAME);
	}
	
	/**
	 * Overrides super method to add the following behaviour
	 * - do not try to match a desktop slot with a mobile request or the reverse
	 * - when matching mobile capabilities, only consider platform_version (the operating system), not the browser version
	 * - allow mobile slots to expose several browser and match against any of these browsers
	 */
	@Override
	public boolean matches(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
		
		if (providedCapabilities == null || requestedCapabilities == null) {
			return false;
		}
		
		String nodePlatformName = (String) providedCapabilities.get(CapabilityType.PLATFORM_NAME);
		boolean mobileNode = "android".equalsIgnoreCase(nodePlatformName) || "ios".equalsIgnoreCase(nodePlatformName) ? true: false;

		String requestedPlatform;
		if (requestedCapabilities.get(CapabilityType.PLATFORM) == null) {
			if (requestedCapabilities.get(CapabilityType.PLATFORM_NAME) == null) {
				requestedPlatform = null;
			} else {
				requestedPlatform = requestedCapabilities.get(CapabilityType.PLATFORM_NAME).toString();
			}
		} else {
			requestedPlatform = requestedCapabilities.get(CapabilityType.PLATFORM).toString();
		}
		boolean mobileRequested = "android".equalsIgnoreCase(requestedPlatform) || "ios".equalsIgnoreCase(requestedPlatform) ? true: false;
		
		// exclude mobile slot if we are searching for desktop one or desktop slot if we are searching for mobile
		if (mobileNode != mobileRequested) {
			return false;
		}
		
		// in case we search mobile node, remove CapabilityType.VERSION (aka browser version) from requested capabilities as only PLATFORM_VERSION should be used
		Map<String, Object> tmpRequestedCapabilities = new HashMap<>(requestedCapabilities);
		if (mobileRequested) {
			tmpRequestedCapabilities.remove(CapabilityType.VERSION);
		}
		
		// issue #44: if restrictToTags is true we look at declared nodeTags
		if (providedCapabilities.get(LaunchConfig.RESTRICT_TO_TAGS) != null && (Boolean)providedCapabilities.get(LaunchConfig.RESTRICT_TO_TAGS) && tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.NODE_TAGS) == null) {
			return false;
		}
		
		// exclude slot if a tag is requested and no tag of the slot matches
		if (tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.NODE_TAGS) != null) {
			try {
				List<String> requestedTags = (List<String>)tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.NODE_TAGS);
				List<String> slotTags = (List<String>)providedCapabilities.get(SeleniumRobotCapabilityType.NODE_TAGS);
				
				if (slotTags == null) {
					return false;
				}
				for (String tag: requestedTags) {
					if (!slotTags.contains(tag)) {
						return false;
					}
				}
			} catch (ClassCastException e) {
				logger.info("requested tags MUST be a list of strings, not a string");
			}
		}
		
		// handle multi browser support for mobile devices
		// browserName can take several values, separated by commas. If device is installed with browser, match is OK
		// get requested browser
		Object providedBrowsers = Stream.of(CapabilityType.BROWSER_NAME, "browser")
		          .map(providedCapabilities::get)
		          .filter(Objects::nonNull)
		          .findFirst()
		          .orElse(null);
		
		if (providedBrowsers == null) {
			return super.matches(providedCapabilities, tmpRequestedCapabilities);
		} else {			
			
			// if node contains browser reference, check that the requested browser is installed among the list
			for (String browser: ((String)providedBrowsers).split(",")) {
				Map<String, Object> tmpProvidedCapabilities = new HashMap<>(providedCapabilities);
				tmpProvidedCapabilities.put(CapabilityType.BROWSER_NAME, browser);
				
				// check beta capability of browser
				if (tmpProvidedCapabilities.containsKey(SeleniumRobotCapabilityType.BETA_BROWSER)
						&& tmpRequestedCapabilities.containsKey(SeleniumRobotCapabilityType.BETA_BROWSER)
						&& tmpProvidedCapabilities.get(SeleniumRobotCapabilityType.BETA_BROWSER) != tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.BETA_BROWSER)) {
					return false;
				}
				
				if (super.matches(tmpProvidedCapabilities, tmpRequestedCapabilities)) {
					return true;
				}
			}
			return false;
		}
	}
}