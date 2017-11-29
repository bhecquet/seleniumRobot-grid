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
import java.util.List;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DefaultDriverSessions;
import org.openqa.selenium.remote.server.DriverProvider;
import org.openqa.selenium.safari.SafariDriver;

import com.google.common.collect.ImmutableList;
import com.infotel.seleniumrobot.grid.AppiumDriverProvider;
import com.infotel.seleniumrobot.grid.CustomDriverProvider;
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
		
		logger.info("adding custom and mobile driver providers");
		
		DesiredCapabilities androidCaps = new DesiredCapabilities();
		androidCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
		androidCaps.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		
		DesiredCapabilities androidChromeCaps = new DesiredCapabilities(androidCaps);
		androidChromeCaps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		
		DesiredCapabilities androidBrowserCaps = new DesiredCapabilities(androidCaps);
		androidChromeCaps.setCapability(CapabilityType.BROWSER_NAME, "browser");
		
		DesiredCapabilities iosCaps = new DesiredCapabilities();
		iosCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "ios");
		iosCaps.setCapability(CapabilityType.PLATFORM, Platform.MAC);
		
		DesiredCapabilities iosSafariCaps = new DesiredCapabilities(iosCaps);
		iosSafariCaps.setCapability(CapabilityType.BROWSER_NAME, "safari");
		
		ImmutableList.Builder<DriverProvider> driverProvidersBuilder = new ImmutableList.Builder<DriverProvider>()
				// desktop drivers
			      .add(new CustomDriverProvider(DesiredCapabilities.firefox(), FirefoxDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.chrome(), ChromeDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.internetExplorer(), InternetExplorerDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.edge(), EdgeDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.opera(), OperaDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.operaBlink(), OperaDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.safari(), SafariDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.phantomjs(), PhantomJSDriver.class))
			      .add(new CustomDriverProvider(DesiredCapabilities.htmlUnit(), HtmlUnitDriver.class));
		
		try {
			new LocalAppiumLauncher();
			
			
				      
				   // mobile drivers
			driverProvidersBuilder.add(new AppiumDriverProvider(androidCaps, AndroidDriver.class))
		      .add(new AppiumDriverProvider(androidBrowserCaps, AndroidDriver.class))
		      .add(new AppiumDriverProvider(androidChromeCaps, AndroidDriver.class))
		      .add(new AppiumDriverProvider(iosCaps, IOSDriver.class))
		      .add(new AppiumDriverProvider(iosSafariCaps, IOSDriver.class));
			
			
			logger.info("appium provider successfuly configured");
		} catch (ConfigurationException e) {
			logger.info("No appium driver provider configured: " + e.getMessage());
		}
		
		List<DriverProvider> driverProviders = driverProvidersBuilder.build();
		Field driverProvidersField = DefaultDriverSessions.class.getDeclaredField("defaultDriverProviders");
		driverProvidersField.setAccessible(true);
		driverProvidersField.set(DefaultDriverSessions.class, driverProviders);
		
	}
}
