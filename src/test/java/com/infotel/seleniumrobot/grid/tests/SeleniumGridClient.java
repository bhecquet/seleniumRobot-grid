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
package com.infotel.seleniumrobot.grid.tests;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

//import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

public class SeleniumGridClient {

	public static void main(String [] args) throws IOException {
		DesiredCapabilities capability = new DesiredCapabilities();
		capability.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		System.setProperty("webdriver.gecko.driver", "/home/worm/workspace/seleniumRobot-drivers/seleniumRobot-linux-driver/target/classes/drivers/linux/geckodriver");
//		capability.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, Arrays.asList("bar"));
//		capability.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
//		capability.setCapability(MobileCapabilityType.DEVICE_NAME, "Google Nexus 6 - 6.0.0 - API 23 - 1440x2560");
		
		capability.setPlatform(Platform.VISTA);
		
		GeckoDriverService gecko = new GeckoDriverService(new File("/home/worm/workspace/seleniumRobot-drivers/seleniumRobot-linux-driver/target/classes/drivers/linux/geckodriver"), 
				4444, ImmutableList.of("--connect-existing", "--marionette-port=2828"), ImmutableMap.of());
		
		WebDriver driver2 = new FirefoxDriver();
		
		WebDriver driver = new FirefoxDriver(gecko);
		driver.manage().timeouts().implicitlyWait(1, TimeUnit.MINUTES);
		driver.get("http://www.google.com");
		driver.findElement(By.name("q")).sendKeys("test");
		
//		WebDriver driver = new RemoteWebDriver(new URL("http://SN782980:4444/wd/hub"), capability);
//		driver.quit();
	}
}
