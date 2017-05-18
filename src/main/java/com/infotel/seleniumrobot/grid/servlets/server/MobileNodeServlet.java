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
package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DefaultDriverProvider;
import org.openqa.selenium.remote.server.DefaultDriverSessions;
import org.openqa.selenium.remote.server.DriverProvider;

import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.mobile.MobileDeviceSelector;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.util.osutility.OSUtilityFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;

/**
 * Servlet for getting all mobile devices information
 * This helps the hub to update capabilities with information on the connected device
 * @author behe
 *
 */
public class MobileNodeServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 216473127866019518L;

	private transient MobileDeviceSelector deviceSelector = new MobileDeviceSelector();

	private static final Logger logger = Logger.getLogger(MobileNodeServlet.class);
	
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		updateDrivers();
		Map<String, String[]> parameters = req.getParameterMap();
		DesiredCapabilities caps = new DesiredCapabilities();
		for (Entry<String, String[]> entry: parameters.entrySet()) {
			caps.setCapability(entry.getKey(), entry.getValue()[0]);
		}
		
		// update capabilities from real device properties (e.g: deviceName value is changed to the ID on android)
		deviceSelector.initialize();
		
		DesiredCapabilities updatedCaps = new DesiredCapabilities();
		try {
			updatedCaps = deviceSelector.updateCapabilitiesWithSelectedDevice(caps);
		} catch (ConfigurationException e) {
			sendError(resp, e.getMessage());
			return;
		}
		
		// add chromedriver path to capabilities when using android
		if (updatedCaps.getCapability(MobileCapabilityType.PLATFORM_NAME) != null && "android".equalsIgnoreCase(updatedCaps.getCapability(MobileCapabilityType.PLATFORM_NAME).toString())) {
			updatedCaps.setCapability("chromedriverExecutable", Paths.get(Utils.getDriverDir().toString(), "chromedriver" + OSUtilityFactory.getInstance().getProgramExtension()).toString());
		}

		resp.setContentType("application/json");
		try (
            ServletOutputStream outputStream = resp.getOutputStream()) {
			
			// prevent "plaform" key to be serialized
			Map<String, String> capsStrings = new HashMap<>();
			for (Entry<String, ?> entry: updatedCaps.asMap().entrySet()) {
				capsStrings.put(entry.getKey(), entry.getValue().toString());
			}
			
			resp.getOutputStream().print(new JSONObject(capsStrings).toString());
        } catch (IOException e) {
        	logger.error("Error sending reply", e);
        }
    }
	
	private void sendError(HttpServletResponse resp, String msg) throws IOException {
		try {
	        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
	        resp.getOutputStream().print(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	private void updateDrivers() {
		
		DesiredCapabilities androidCaps = new DesiredCapabilities();
		androidCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
		
		DesiredCapabilities androidChromeCaps = new DesiredCapabilities(androidCaps);
		androidChromeCaps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		
		DesiredCapabilities iosCaps = new DesiredCapabilities();
		androidCaps.setCapability(MobileCapabilityType.PLATFORM_NAME, "ios");
		
		DesiredCapabilities iosSafariCaps = new DesiredCapabilities(androidCaps);
		androidChromeCaps.setCapability(CapabilityType.BROWSER_NAME, "safari");
		
		DriverProvider appiumAndroidProvider = new DefaultDriverProvider(androidCaps, AndroidDriver.class);
		DriverProvider appiumAndroidChromeProvider = new DefaultDriverProvider(androidChromeCaps, AndroidDriver.class);
		DriverProvider appiumIosProvider = new DefaultDriverProvider(iosCaps, IOSDriver.class);
		DriverProvider appiumIosSafariProvider = new DefaultDriverProvider(iosSafariCaps, IOSDriver.class);
		
		try {
			Field driverProvidersField = DefaultDriverSessions.class.getDeclaredField("defaultDriverProviders");
			driverProvidersField.setAccessible(true);
			List<DriverProvider> driverList = (List<DriverProvider>)driverProvidersField.get(DefaultDriverSessions.class);
			List<DriverProvider> newDriverList = new ArrayList<>(driverList);
			newDriverList.add(appiumAndroidChromeProvider);
			newDriverList.add(appiumAndroidProvider);
			newDriverList.add(appiumIosProvider);
			newDriverList.add(appiumIosSafariProvider);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
