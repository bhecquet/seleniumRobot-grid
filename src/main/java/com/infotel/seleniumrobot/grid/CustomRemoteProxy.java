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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.osutility.OSUtilityFactory;

import io.appium.java_client.remote.MobileCapabilityType;

public class CustomRemoteProxy extends DefaultRemoteProxy {

	public CustomRemoteProxy(RegistrationRequest request, Registry registry) {
		super(request, registry);
	}
	
	@Override
	public void beforeSession(TestSession session) {
		super.beforeSession(session);
		
		// update capabilities for mobile. Mobile tests are identified by the use of 'platformName' capability
		if (session.getRequestedCapabilities().containsKey(MobileCapabilityType.PLATFORM_NAME)) {
			MobileNodeServletClient mobileServletClient = new MobileNodeServletClient(session.getSlot().getRemoteURL().getHost(), session.getSlot().getRemoteURL().getPort());
			try {
				DesiredCapabilities caps = mobileServletClient.updateCapabilities(new DesiredCapabilities(session.getRequestedCapabilities()));
				session.getRequestedCapabilities().putAll(caps.asMap());
			} catch (IOException | URISyntaxException e) {
			}
			
			// add chromedriver path to capabilities when using android
			if ("android".equalsIgnoreCase(session.getRequestedCapabilities().get(MobileCapabilityType.PLATFORM_NAME).toString())) {
				session.getRequestedCapabilities().put("chromedriverExecutable", Paths.get(Utils.getDriverDir().toString(), "chromedriver" + OSUtilityFactory.getInstance().getProgramExtension()).toString());
			}
		}
	}

}
