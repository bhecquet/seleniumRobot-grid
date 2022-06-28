package com.infotel.seleniumrobot.grid.tasks;

import java.util.ArrayList;
import java.util.List;

import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.util.osutility.OSUtility;

public class DiscoverBrowserAndDriverPidsTask implements Task {

	private String browserName;
	private String browserVersion;
	private List<Long> parentsPids;
	private List<Long> existingPids;
	private List<Long> processPids;
	
	public DiscoverBrowserAndDriverPidsTask(String browserName, String browserVersion) {
		this.browserName = browserName;
		this.browserVersion = browserVersion;
	}
	
	/**
	 * If set, we will get the list of PIDs for current browser, excluding PIDs already existing previously
	 * @param existingPids
	 * @return
	 */
	public DiscoverBrowserAndDriverPidsTask withExistingPids(List<Long> existingPids) {
		this.existingPids = existingPids;
		return this;
	}
	
	/**
	 * If set, we will get all driver and browser PIDs that are children of PIDs given as parameters, plus all subprocess 
	 * @param parentsPids
	 * @return
	 */
	public DiscoverBrowserAndDriverPidsTask withParentsPids(List<Long> parentsPids) {
		this.parentsPids = parentsPids;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DiscoverBrowserAndDriverPidsTask execute() throws Exception {
		BrowserInfo browserInfo = getBrowserInfo(browserName, browserVersion);
		
		if (parentsPids != null) {
			processPids = new ArrayList<>();
			if (browserInfo != null) {
				processPids.addAll(browserInfo.getAllBrowserSubprocessPids(parentsPids));
			}
		} else if (existingPids != null) {

			if (browserInfo != null) {
				processPids.addAll(browserInfo.getDriverAndBrowserPid(existingPids));
	    	}
		}
		
		return this;
	}
	

	/**
	 * Returns BrowserInfo corresponding to this browser name and version
	 * @param browserName
	 * @param browserVersion
	 * @return
	 */
	private BrowserInfo getBrowserInfo(String browserName, String browserVersion) {
		List<BrowserInfo> browserInfos = OSUtility.getInstalledBrowsersWithVersion().get( 
				com.seleniumtests.driver.BrowserType.getBrowserTypeFromSeleniumBrowserType(browserName));
		if (browserInfos == null) {
			return null;
		}
		
		
		// select the right browserInfo depending on browser version
		BrowserInfo browserInfo = null;
		for (BrowserInfo bi: browserInfos) {
			browserInfo = bi; // get at least one of the browserInfo
			if (bi.getVersion().equals(browserVersion)) {
				break;
			}
		}
		return browserInfo;
	}

	public List<Long> getProcessPids() {
		return processPids;
	}

}
