package com.infotel.seleniumrobot.grid.distributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.grid.data.DefaultSlotMatcher;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.CapabilityType;

import com.infotel.seleniumrobot.grid.aspects.SessionSlotActions;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

public class SeleniumRobotSlotMatcher extends DefaultSlotMatcher {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 13545464218L;
	private static Logger logger = LogManager.getLogger(SeleniumRobotSlotMatcher.class);

	@Override
	public boolean matches(Capabilities providedCapabilities /* stereotype */, Capabilities requestedCapabilities) {
		
		if (providedCapabilities == null || requestedCapabilities == null) {
			return false;
		}
		
		Platform nodePlatformName = providedCapabilities.getPlatformName();
		boolean mobileNode = nodePlatformName.is(Platform.ANDROID) || nodePlatformName.is(Platform.IOS) ? true: false;

		String requestedPlatform = requestedCapabilities.getPlatformName() != null ? requestedCapabilities.getPlatformName().toString(): null;

		boolean mobileRequested = Platform.ANDROID.toString().equalsIgnoreCase(requestedPlatform) || Platform.IOS.toString().equalsIgnoreCase(requestedPlatform) ? true: false;
		
		// exclude mobile slot if we are searching for desktop one or desktop slot if we are searching for mobile
		if (mobileNode != mobileRequested) {
			return false;
		}
		
		// in case we search mobile node, remove CapabilityType.VERSION (aka browser version) from requested capabilities as only PLATFORM_VERSION should be used
		Map<String, Object> tmpRequestedCapabilities = new HashMap<>(requestedCapabilities.asMap());
		if (mobileRequested) {
			tmpRequestedCapabilities.remove(CapabilityType.BROWSER_VERSION);
		}
		
		// issue #44: if restrictToTags is true we look at declared nodeTags
		if (providedCapabilities.getCapability(LaunchConfig.RESTRICT_TO_TAGS) != null 
				&& (Boolean)providedCapabilities.getCapability(LaunchConfig.RESTRICT_TO_TAGS) 
				&& tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.NODE_TAGS) == null) {
			return false;
		}
		
		// exclude slot if a tag is requested and no tag of the slot matches
		if (tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.NODE_TAGS) != null) {
			try {
				List<String> requestedTags = (List<String>)tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.NODE_TAGS);
				List<String> slotTags = (List<String>)providedCapabilities.getCapability(SeleniumRobotCapabilityType.NODE_TAGS);
				
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
		// remove NODE_TAGS to avoid a match as this is a special case
		tmpRequestedCapabilities.remove(SeleniumRobotCapabilityType.NODE_TAGS);
		
		// handle multi browser support for mobile devices
		// browserName can take several values, separated by commas. If device is installed with browser, match is OK
		// get requested browser
		Object providedBrowsers = Stream.of(CapabilityType.BROWSER_NAME, "browser")
		          .map(providedCapabilities::getCapability)
		          .filter(Objects::nonNull)
		          .findFirst()
		          .orElse(null);
		
		if (providedBrowsers == null) {
			return super.matches(providedCapabilities, new MutableCapabilities(tmpRequestedCapabilities));
		} else {			
			
			// if node contains browser reference, check that the requested browser is installed among the list
			for (String browser: ((String)providedBrowsers).split(",")) {
				Map<String, Object> tmpProvidedCapabilities = new HashMap<>(providedCapabilities.asMap());
				tmpProvidedCapabilities.put(CapabilityType.BROWSER_NAME, browser);
				
				// check beta capability of browser
				if (tmpProvidedCapabilities.containsKey(SeleniumRobotCapabilityType.BETA_BROWSER)
						&& tmpRequestedCapabilities.containsKey(SeleniumRobotCapabilityType.BETA_BROWSER)
						&& tmpProvidedCapabilities.get(SeleniumRobotCapabilityType.BETA_BROWSER) != tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.BETA_BROWSER)) {
					continue;
				}
				
				// check Edge in IE mode through "edgePath" capability. This is assured to be there with GridStarted
				// if edgePath is null, IE is there, but not Edge
				if (tmpRequestedCapabilities.containsKey(SeleniumRobotCapabilityType.EDGE_IE_MODE) 
						&& (boolean) tmpRequestedCapabilities.get(SeleniumRobotCapabilityType.EDGE_IE_MODE) 
						&& Browser.IE.browserName().toString().equals(browser) 
						&& tmpProvidedCapabilities.get(SessionSlotActions.EDGE_PATH) == null) {
					continue;
				}
				
				if (super.matches(new MutableCapabilities(tmpProvidedCapabilities), new MutableCapabilities(tmpRequestedCapabilities))) {
					return true;
				}
			}
			return false;
		}
	}
}
