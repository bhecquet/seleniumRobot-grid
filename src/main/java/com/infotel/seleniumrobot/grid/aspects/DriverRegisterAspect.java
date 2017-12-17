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

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DefaultDriverFactory;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

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
	
	@Around("call(private void org.openqa.selenium.remote.server.DefaultDriverFactory.registerDefaults (..))")
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
		
		DefaultDriverFactory driverFactory = ((DefaultDriverFactory)joinPoint.getTarget());
		driverFactory.registerDriverProvider(new CustomDriverProvider(DesiredCapabilities.firefox(), FirefoxDriver.class));
		
	    driverFactory.registerDriverProvider(new CustomDriverProvider(new ChromeOptions(), ChromeDriver.class));
	    driverFactory.registerDriverProvider(new CustomDriverProvider(new InternetExplorerOptions(), InternetExplorerDriver.class));
	    driverFactory.registerDriverProvider(new CustomDriverProvider(new EdgeOptions(), EdgeDriver.class));
	    driverFactory.registerDriverProvider(new CustomDriverProvider(DesiredCapabilities.opera(), OperaDriver.class));
	    driverFactory.registerDriverProvider(new CustomDriverProvider(new OperaOptions(), OperaDriver.class));
	    driverFactory.registerDriverProvider(new CustomDriverProvider(new SafariOptions(), SafariDriver.class));
	    driverFactory.registerDriverProvider(new CustomDriverProvider(DesiredCapabilities.htmlUnit(), HtmlUnitDriver.class));
		
		try {
			new LocalAppiumLauncher();
			      
			// mobile drivers
			driverFactory.registerDriverProvider(new AppiumDriverProvider(androidCaps, AndroidDriver.class));
		    driverFactory.registerDriverProvider(new AppiumDriverProvider(androidBrowserCaps, AndroidDriver.class));
		    driverFactory.registerDriverProvider(new AppiumDriverProvider(androidChromeCaps, AndroidDriver.class));
		    driverFactory.registerDriverProvider(new AppiumDriverProvider(iosCaps, IOSDriver.class));
		    driverFactory.registerDriverProvider(new AppiumDriverProvider(iosSafariCaps, IOSDriver.class));
			
			
			logger.info("appium provider successfuly configured");
		} catch (ConfigurationException e) {
			logger.info("No appium driver provider configured: " + e.getMessage());
		}
		
	}
}
