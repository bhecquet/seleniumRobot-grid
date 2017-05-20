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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DefaultDriverProvider;

import com.infotel.seleniumrobot.grid.servlets.client.FileServletClient;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.mobile.AppiumLauncher;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;

import io.appium.java_client.remote.MobileCapabilityType;

public class AppiumDriverProvider extends DefaultDriverProvider {
	
	private static final Logger LOG = Logger.getLogger(DefaultDriverProvider.class.getName());

	private Class<? extends WebDriver> driverClass;
	
	public AppiumDriverProvider(Capabilities capabilities, Class<? extends WebDriver> driverClass) {
		super(capabilities, driverClass);
	    this.driverClass = driverClass;
	}

	@Override
	public WebDriver newInstance(Capabilities capabilities) {
		LOG.info("Creating a new session for " + capabilities);
		String logDir = Paths.get(Utils.getRootdir(), "logs", "appium").toString();

		// start appium before creating instance
		AppiumLauncher appiumLauncher = new LocalAppiumLauncher(logDir);
    	appiumLauncher.startAppium();
		
		// Try and call the single arg constructor that takes a capabilities
		// first
		return callConstructor(driverClass, capabilities, appiumLauncher);
	}

	private WebDriver callConstructor(Class<? extends WebDriver> from, Capabilities capabilities, AppiumLauncher appiumLauncher) {
		Constructor<? extends WebDriver> constructor;
		try {
			constructor = from.getConstructor(URL.class, Capabilities.class);
			return constructor.newInstance(new URL(((LocalAppiumLauncher)appiumLauncher).getAppiumServerUrl()), capabilities);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException e) {
			throw new WebDriverException(e);
		}
			
			
	}


}
