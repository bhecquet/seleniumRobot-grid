package com.infotel.seleniumrobot.grid.aspects;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DefaultDriverSessions;
import org.openqa.selenium.remote.server.DriverProvider;

import com.infotel.seleniumrobot.grid.AppiumDriverProvider;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;

@Aspect
public class DriverRegisterAspect {
	

	@Before("call(private void org.openqa.selenium.remote.server.DefaultDriverSessions.registerDefaults (..))")
	public void changeDriver(JoinPoint joinPoint) throws Throwable {
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
		System.out.println("coucou");
	}
}
