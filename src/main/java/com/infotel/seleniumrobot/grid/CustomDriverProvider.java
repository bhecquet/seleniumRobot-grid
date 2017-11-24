package com.infotel.seleniumrobot.grid;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.server.DefaultDriverProvider;

import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.util.osutility.OSUtility;

public class CustomDriverProvider extends DefaultDriverProvider {
	
	private static final Logger LOG = Logger.getLogger(DefaultDriverProvider.class.getName());
	
	private Class<? extends WebDriver> driverClass;
	private List<Long> driverPids;
	private static final Object lock = new Object();

	public CustomDriverProvider(Capabilities capabilities, Class<? extends WebDriver> driverClass) {
		super(capabilities, driverClass);
		this.driverClass = driverClass;
	}

	@Override
	public WebDriver newInstance(Capabilities capabilities) {
		LOG.info("Creating a new session for " + capabilities);
		// Try and call the single arg constructor that takes a capabilities first
		synchronized (lock) {
			
			// get browser info used to start this driver. It will be used then for 
        	BrowserInfo browserInfo = OSUtility.getInstalledBrowsersWithVersion().get( 
        			com.seleniumtests.driver.BrowserType.getBrowserTypeFromSeleniumBrowserType((String)(capabilities.getCapability(CapabilityType.BROWSER_NAME))));
        	List<Long> existingPids = new ArrayList<>();

    		// get pid pre-existing the creation of this driver. This helps filtering drivers launched by other tests or users
    		if (browserInfo != null) {
        		existingPids.addAll(browserInfo.getDriverAndBrowserPid(new ArrayList<>()));
        	}
			
			WebDriver driver = callConstructor(driverClass, capabilities);
			

            // get the created PIDs
            if (browserInfo != null) {
    			driverPids = browserInfo.getDriverAndBrowserPid(existingPids);
    		}
			
			return driver;
		}
		
	}

	private WebDriver callConstructor(Class<? extends WebDriver> from, Capabilities capabilities) {
		
		// set properties depending on browser / browser version
		if (BrowserType.CHROME.equals(capabilities.getCapability(CapabilityType.BROWSER_NAME)) && capabilities.getCapability(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY) != null) {
			System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, capabilities.getCapability(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).toString());
		} else if (BrowserType.FIREFOX.equals(capabilities.getCapability(CapabilityType.BROWSER_NAME)) && capabilities.getCapability(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY) != null) {
			System.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, capabilities.getCapability(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY).toString());
		} else if (BrowserType.IE.equals(capabilities.getCapability(CapabilityType.BROWSER_NAME)) && capabilities.getCapability(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY) != null) {
			System.setProperty(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, capabilities.getCapability(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY).toString());
		} else if (BrowserType.EDGE.equals(capabilities.getCapability(CapabilityType.BROWSER_NAME)) && capabilities.getCapability(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY) != null) {
			System.setProperty(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY, capabilities.getCapability(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY).toString());
		}
		
		try {
			Constructor<? extends WebDriver> constructor = from.getConstructor(Capabilities.class);
			return constructor.newInstance(capabilities);
		} catch (NoSuchMethodException e) {
			try {
				return from.newInstance();
			} catch (ReflectiveOperationException e1) {
				throw new WebDriverException(e);
			}
		} catch (ReflectiveOperationException e) {
			throw new WebDriverException(e);
		}
	}

}
