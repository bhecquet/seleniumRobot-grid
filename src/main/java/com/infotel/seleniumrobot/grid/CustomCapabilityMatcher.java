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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.openqa.grid.internal.utils.CapabilityMatcher;

//Licensed to the Software Freedom Conservancy (SFC) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The SFC licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;

import io.appium.java_client.remote.MobileCapabilityType;

/**
 * Default (naive) implementation of the capability matcher.
 * <p>
 * The default capability matcher will look at all the key from the request do
 * not start with _ and will try to find a node that has at least those
 * capabilities.
 */
public class CustomCapabilityMatcher implements CapabilityMatcher {

	private static final Logger log = Logger.getLogger(CustomCapabilityMatcher.class.getName());
	private static final String GRID_TOKEN = "_";

	protected final List<String> toConsider = new ArrayList<>();

	public CustomCapabilityMatcher() { 
		toConsider.add(CapabilityType.PLATFORM);
		toConsider.add(CapabilityType.BROWSER_NAME);
		toConsider.add(CapabilityType.VERSION);
		toConsider.add(MobileCapabilityType.PLATFORM_NAME);
		toConsider.add(MobileCapabilityType.PLATFORM_VERSION);
		toConsider.add(MobileCapabilityType.DEVICE_NAME);
	}

	/**
	 * @param capabilityName
	 *            capability name to have grid match requested with test slot
	 */
	public void addToConsider(String capabilityName) {
		toConsider.add(capabilityName);
	}

	@Override
	public boolean matches(Map<String, Object> nodeCapability, Map<String, Object> requestedCapability) {

		if (nodeCapability == null || requestedCapability == null) {
			return false;
		}

		for (Entry<String, Object> entry : requestedCapability.entrySet()) {
			
			String key = entry.getKey();
			if (
					// ignore capabilities that are targeted at grid internal for the matching
					key.startsWith(GRID_TOKEN) 
					|| !toConsider.contains(key) 
					|| requestedCapability.get(key) == null
				) {
				continue;
			}
			
			String value = requestedCapability.get(key).toString();

			if (!("ANY".equalsIgnoreCase(value) || "".equals(value) || "*".equals(value))) {
				Platform requested = extractPlatform(requestedCapability.get(key));
				
				// special case for platform
				if (requested != null) {
					
					Platform node = extractPlatform(nodeCapability.get(key));
					if (node == null) {
						return false;
					}
					
					// check we have the same platform, or at least the same family, if the family only is requested
					// if windows is requested, it should match with any of XP, VISTA, WIN8, ...
					if (!node.is(requested)) {
						return false;
					}
				} else {
					
					// handle multi browser support for mobile devices
					// browserName can take several values, seperated by commas. If device is installed with browser, match is OK
					if (CapabilityType.BROWSER_NAME.equals(key)) {
						boolean browserFound = false;
						for (String browser: ((String)nodeCapability.get(key)).split(",")) {
							if (requestedCapability.get(key).equals(browser)) {
								browserFound = true;
							}
						}
						if (!browserFound) {
							return false;
						}
					} else if (!requestedCapability.get(key).equals(nodeCapability.get(key))) {
						return false;
					}
				}
			} 
		}
		return true;
	}

	Platform extractPlatform(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Platform) {
			return (Platform) o;
		} else if (o instanceof String) {
			String name = o.toString();
			try {
				return Platform.valueOf(name);
			} catch (IllegalArgumentException e) {
				// no exact match, continue to look for a partial match
			}
			for (Platform os : Platform.values()) {
				for (String matcher : os.getPartOfOsName()) {
					if ("".equals(matcher))
						continue;
					if (name.equalsIgnoreCase(matcher)) {
						return os;
					}
				}
			}
			return null;
		} else {
			return null;
		}
	}
}