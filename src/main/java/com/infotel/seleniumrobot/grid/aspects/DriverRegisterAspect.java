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
package com.infotel.seleniumrobot.grid.aspects;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DefaultDriverSessions;
import org.openqa.selenium.remote.server.DriverProvider;

import com.infotel.seleniumrobot.grid.AppiumDriverProvider;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;
import com.seleniumtests.customexception.ConfigurationException;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;

@Aspect
public class DriverRegisterAspect {
	
	private static final Logger logger = Logger.getLogger(DriverRegisterAspect.class);
	
	@Before("call(private void org.openqa.selenium.remote.server.DefaultDriverSessions.registerDefaults (..))")
	public void changeDriver(JoinPoint joinPoint) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		
		logger.info("adding mobile  driver providers");
		
		DesiredCapabilities androidCaps = new DesiredCapabilities();
		androidCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
		androidCaps.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		
		DesiredCapabilities androidChromeCaps = new DesiredCapabilities(androidCaps);
		androidChromeCaps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		
		DesiredCapabilities iosCaps = new DesiredCapabilities();
		iosCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "ios");
		iosCaps.setCapability(CapabilityType.PLATFORM, Platform.MAC);
		
		DesiredCapabilities iosSafariCaps = new DesiredCapabilities(iosCaps);
		iosSafariCaps.setCapability(CapabilityType.BROWSER_NAME, "safari");
		
		try {
			new LocalAppiumLauncher();
			
			DriverProvider appiumAndroidProvider = new AppiumDriverProvider(androidCaps, AndroidDriver.class);
			DriverProvider appiumAndroidChromeProvider = new AppiumDriverProvider(androidChromeCaps, AndroidDriver.class);
			DriverProvider appiumIosProvider = new AppiumDriverProvider(iosCaps, IOSDriver.class);
			DriverProvider appiumIosSafariProvider = new AppiumDriverProvider(iosSafariCaps, IOSDriver.class);
			
			Field driverProvidersField = DefaultDriverSessions.class.getDeclaredField("defaultDriverProviders");
			driverProvidersField.setAccessible(true);
			List<DriverProvider> driverList = (List<DriverProvider>)driverProvidersField.get(DefaultDriverSessions.class);
			List<DriverProvider> newDriverList = new ArrayList<>(driverList);
			newDriverList.add(appiumAndroidChromeProvider);
			newDriverList.add(appiumAndroidProvider);
			newDriverList.add(appiumIosProvider);
			newDriverList.add(appiumIosSafariProvider);
			driverProvidersField.set(DefaultDriverSessions.class, newDriverList);
			logger.info("appium provider successfuly configured");
		} catch (ConfigurationException e) {
			logger.info("No appium driver provider configured: " + e.getMessage());
		}
		
	}
}
