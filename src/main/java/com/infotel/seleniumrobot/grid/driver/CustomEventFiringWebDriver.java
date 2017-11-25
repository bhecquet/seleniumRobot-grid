package com.infotel.seleniumrobot.grid.driver;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.util.osutility.OSUtilityFactory;

public class CustomEventFiringWebDriver extends EventFiringWebDriver {
	
	private final List<Long> driverPids;
	private final WebDriver driver;
	private final BrowserInfo browserInfo;

	public CustomEventFiringWebDriver(WebDriver driver) {
		this(driver, new ArrayList<>(), null);
	}
	
	public CustomEventFiringWebDriver(WebDriver driver, List<Long> driverPids, BrowserInfo browserInfo) {
		super(driver);
		this.driverPids = driverPids;
		this.driver = driver;
		this.browserInfo = browserInfo;
	}

	/**
	 * After quitting driver, if it fails, some pids may remain. Kill them
	 */
	@Override
	public void quit() {
		try {
			driver.quit();
		} finally {
		
			List<Long> pidsToKill = browserInfo.getAllBrowserSubprocessPids(driverPids);
	    	for (Long pid: pidsToKill) {
	    		OSUtilityFactory.getInstance().killProcess(pid.toString(), true);
	    	}
		}
	}
	
	

}
