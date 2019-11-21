package com.infotel.seleniumrobot.grid.driver;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.DriverProvider;

import com.infotel.seleniumrobot.grid.utils.Utils;

public abstract class AppiumDriverProvider implements DriverProvider {


	private static final Logger logger = Logger.getLogger(AppiumDriverProvider.class);
	
	@Override
	public Capabilities getProvidedCapabilities() {
		
		return new DesiredCapabilities();
	}

	@Override
	public boolean canCreateDriverInstanceFor(Capabilities capabilities) {
		return false;
	}

	@Override
	public WebDriver newInstance(Capabilities capabilities) {
		logger.info("Creating a new session for " + capabilities);
		String logDir = Paths.get(Utils.getRootdir(), "logs", "appium").toString();

		return callConstructor(getDriverClass(), capabilities);
	}
	

	protected WebDriver callConstructor(Class<? extends WebDriver> from, Capabilities capabilities) {

		Constructor<? extends WebDriver> constructor;
		try {
			constructor = from.getConstructor(URL.class, Capabilities.class);
			return constructor.newInstance(new URL((String) capabilities.getCapability("appiumUrl")), capabilities);
		} catch (Exception e) {
			throw new WebDriverException(e);
		}
	}

	protected abstract Class<? extends WebDriver> getDriverClass();
	
}
