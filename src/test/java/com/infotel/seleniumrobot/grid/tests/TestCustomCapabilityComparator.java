package com.infotel.seleniumrobot.grid.tests;

import java.util.ArrayList;
import java.util.Collection;

import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.CustomCapabilitiesComparator;

import io.appium.java_client.remote.MobileCapabilityType;

public class TestCustomCapabilityComparator {
	
	private static Collection<DesiredCapabilities> nodeCaps = new ArrayList<>();
	private static DesiredCapabilities nodeCaps1 = new DesiredCapabilities();
	private static DesiredCapabilities nodeCaps2 = new DesiredCapabilities();
	private static DesiredCapabilities nodeCaps3 = new DesiredCapabilities();
	private static DesiredCapabilities nodeCaps4 = new DesiredCapabilities();
	
	@BeforeClass(groups={"grid"})
	private static void initNodes() {
		nodeCaps1.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		nodeCaps1.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		nodeCaps.add(nodeCaps1);
		
		nodeCaps2.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		nodeCaps2.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
		nodeCaps2.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		nodeCaps.add(nodeCaps2);
		
		nodeCaps3.setCapability(CapabilityType.PLATFORM, Platform.WINDOWS);
		nodeCaps3.setCapability(CapabilityType.BROWSER_NAME, "firefox");
		nodeCaps.add(nodeCaps3);
		
		nodeCaps4.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		nodeCaps4.setCapability(MobileCapabilityType.PLATFORM_NAME, "ios");
		nodeCaps4.setCapability(CapabilityType.BROWSER_NAME, "safari");
		nodeCaps.add(nodeCaps4);
	}

	@Test(groups={"grid"})
	public void testComparisonWithoutPlatformName() {
		DesiredCapabilities desiredCaps = new DesiredCapabilities();
		desiredCaps.setCapability(CapabilityType.PLATFORM, Platform.WINDOWS);
		desiredCaps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		
		DesiredCapabilities bestCaps = CustomCapabilitiesComparator.getBestMatch(desiredCaps, nodeCaps);
		Assert.assertEquals(bestCaps, nodeCaps1);
	}
	
	@Test(groups={"grid"})
	public void testComparisonWithPlatformNameAndroid() {
		DesiredCapabilities desiredCaps = new DesiredCapabilities();
		desiredCaps.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		desiredCaps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		desiredCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
		
		DesiredCapabilities bestCaps = CustomCapabilitiesComparator.getBestMatch(desiredCaps, nodeCaps);
		Assert.assertEquals(bestCaps, nodeCaps2);
	}
	
	@Test(groups={"grid"})
	public void testComparisonWithPlatformNameIos() {
		DesiredCapabilities desiredCaps = new DesiredCapabilities();
		desiredCaps.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		desiredCaps.setCapability(CapabilityType.BROWSER_NAME, "safari");
		desiredCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "ios");
		
		DesiredCapabilities bestCaps = CustomCapabilitiesComparator.getBestMatch(desiredCaps, nodeCaps);
		Assert.assertEquals(bestCaps, nodeCaps4);
	}
}
