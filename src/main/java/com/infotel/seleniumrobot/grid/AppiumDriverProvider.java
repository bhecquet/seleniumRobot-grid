package com.infotel.seleniumrobot.grid;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.server.DefaultDriverProvider;

import com.seleniumtests.browserfactory.mobile.AppiumLauncher;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;

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
		
		// start appium before creating instance
		AppiumLauncher appiumLauncher = new LocalAppiumLauncher();
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
