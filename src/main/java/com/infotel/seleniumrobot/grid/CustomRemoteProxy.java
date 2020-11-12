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
package com.infotel.seleniumrobot.grid;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.tls.UDPTransport;
import org.json.JSONException;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.handler.RequestType;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.jmx.ManagedService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.FileServlet;
import com.infotel.seleniumrobot.grid.servlets.server.StatusServlet;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.customexception.ConfigurationException;

import io.appium.java_client.remote.MobileCapabilityType;
import kong.unirest.UnirestException;

/**
 * Custom proxy that handles the following features
 * - Set path to driver into node system properties so that driver can be found when browser starts (method: beforeSession)
 * - kill browser and driver processes after the test ends. Pids are detected by difference between list of pids before we create the session and after session has been created
 *  (method: beforeStartSession & afterStartSession)
 * - prepare tests for mobile: application file URL added to capabilities and device capabilities updated to reflect a real device instead of a generic one (e.g: Android) 
 * - start / stop appium for mobile testing (method: afterSession)
 * - restart hub / node when max number of test sessions has been reached (method: afterSession)
 * - manage the hub and node status (ACTIVE or INACTIVE). No new test session will be created on a node if it's inactive. Currently running session will go to end (method: hasCapability)
 * 
 * @author S047432
 *
 */
@ManagedService(description = "Selenium Custom Grid Hub TestSlot")
public class CustomRemoteProxy extends DefaultRemoteProxy {
	

	public static final String ALL_ACCESS = "allAccess";
	public static final String PREEXISTING_DRIVER_PIDS = "preexistingDriverPids";
	public static final String CURRENT_DRIVER_PIDS = "currentDriverPids";
	public static final String PIDS_TO_KILL = "pidsToKill";
	public static final int DEFAULT_LOCK_TIMEOUT = 30;
	private static Integer hubTestSessionCount = 0;
	private static LocalDateTime lowActivityBeginning = null;
	
	private boolean	upgradeAttempted = false;
	private int lockTimeout;
	private int testSessionsCount = 0;
	
	private NodeTaskServletClient nodeClient;
	private NodeStatusServletClient nodeStatusClient;
	private MobileNodeServletClient mobileServletClient;

	private Lock newTestSessionLock;
	
	private static final Logger logger = Logger.getLogger(CustomRemoteProxy.class);

	public CustomRemoteProxy(RegistrationRequest request, GridRegistry registry) {
		super(request, registry);
		init(new NodeTaskServletClient(getRemoteHost().getHost(), getRemoteHost().getPort()),
				new NodeStatusServletClient(getRemoteHost().getHost(), getRemoteHost().getPort()),
				new MobileNodeServletClient(getRemoteHost().getHost(), getRemoteHost().getPort()),
				DEFAULT_LOCK_TIMEOUT);
	}
	
	// for test only
	public CustomRemoteProxy(RegistrationRequest request, 
			GridRegistry registry, 
			NodeTaskServletClient nodeClient, 
			NodeStatusServletClient nodeStatusClient, 
			MobileNodeServletClient mobileServletClient, 
			int lockTimeout) {
		super(request, registry);
		init(nodeClient, nodeStatusClient, mobileServletClient, lockTimeout);
	}
	
	private void init(NodeTaskServletClient nodeClient, 
			NodeStatusServletClient nodeStatusClient, 
			MobileNodeServletClient mobileServletClient, 
			int lockTimeout) {
		this.nodeClient = nodeClient;
		this.nodeStatusClient = nodeStatusClient;
		this.mobileServletClient = mobileServletClient;
		newTestSessionLock = new ReentrantLock();
		this.lockTimeout = lockTimeout;
	}

	@Override
	public void beforeCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
		super.beforeCommand(session, request, response);

		// for new session request, add driver path to capabilities so that they can be read by node
		String body = ((SeleniumBasedRequest)request).getBody();
		try {
			JsonObject map = new JsonParser().parse(body).getAsJsonObject();
			boolean bodyChanged = false;
			if (map.has("capabilities")) {
				map.getAsJsonObject("capabilities").remove("desiredCapabilities");
				map.getAsJsonObject("capabilities").add("desiredCapabilities", new JsonParser().parse(new Gson().toJson(session.getRequestedCapabilities())).getAsJsonObject());
				bodyChanged = true;
			}
			if (map.has("desiredCapabilities")) {
				map.remove("desiredCapabilities");
				map.add("desiredCapabilities", new JsonParser().parse(new Gson().toJson(session.getRequestedCapabilities())).getAsJsonObject());
				bodyChanged = true;
			}
		
			if (bodyChanged) {
				((SeleniumBasedRequest)request).setBody(map.toString());
			}

		} catch (JsonSyntaxException | IllegalStateException | UnsupportedEncodingException  e) {
		}
		
		// get PID before we create driver
		// use locking so that only one session is created at a time
		if(((SeleniumBasedRequest)request).getRequestType() == RequestType.START_SESSION) {
			beforeStartSession(session);
		}
		
		else if(((SeleniumBasedRequest)request).getRequestType() == RequestType.STOP_SESSION) {
			beforeStopSession(session);
		}
	}
	

	/**
	 * Do something after the START_SESSION and STOP_SESSION commands has been sent
	 */
	@Override
	public void afterCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
		super.afterCommand(session, request, response);
		
		
		if(((SeleniumBasedRequest)request).getRequestType() == RequestType.START_SESSION) {
			afterStartSession(session);			
		}
		
		else if(((SeleniumBasedRequest)request).getRequestType() == RequestType.STOP_SESSION && session.get(PIDS_TO_KILL) != null) {
			afterStopSession(session);
		}
	}
	
	/**
	 * Do actions before test session creation
	 * - add device name when doing mobile testing. This will allow to add missing caps, for example when client requests an android device without specifying it precisely
	 * - make file available as HTTP URL instead of FILE URL. These files must have already been uploaded to grid hub
	 *  
	 */
	@Override
	public void beforeSession(TestSession session) {
		
		// add firefox & chrome binary to caps
		super.beforeSession(session);
		boolean mobilePlatform = false;
		Map<String, Object> requestedCaps = session.getRequestedCapabilities();
		
		// update capabilities for mobile. Mobile tests are identified by the use of 'platformName' capability
		// this will allow to add missing caps, for example when client requests an android device without specifying it precisely
		String platformName = (String)requestedCaps.getOrDefault(MobileCapabilityType.PLATFORM_NAME, "nonmobile");
		if (platformName.toLowerCase().contains("ios") || platformName.toLowerCase().contains("android")) {
			mobilePlatform = true;
			try {
				DesiredCapabilities caps = mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps));
				requestedCaps.putAll(caps.asMap());
			} catch (IOException | URISyntaxException e) {
			}
			
			try {
				String appiumUrl = nodeClient.startAppium(session.getInternalKey());
				requestedCaps.put("appiumUrl", appiumUrl);
			} catch (UnirestException e) {
				throw new ConfigurationException("Could not start appium: " + e.getMessage());
			}
		}

		// replace all capabilities whose value begins with 'file:' by the remote HTTP URL
		// we assume that these files have been previously uploaded on hub and thus available
		for (Entry<String, Object> entry: session.getRequestedCapabilities().entrySet()) {
			if (entry.getValue() instanceof String && ((String)entry.getValue()).startsWith(FileServlet.FILE_PREFIX)) {
				requestedCaps.put(entry.getKey(), String.format("http://%s:%s/grid/admin/FileServlet/%s", 
																getConfig().getHubHost(), 
																getConfig().getHubPort(), 
																((String)entry.getValue()).replace(FileServlet.FILE_PREFIX, "")));
			}
		}
		
		// set marionette mode depending on firefox version
		Map<String, Object> nodeCapabilities = session.getSlot().getCapabilities();
		if (nodeCapabilities.get(CapabilityType.BROWSER_NAME) != null 
				&& nodeCapabilities.get(CapabilityType.BROWSER_NAME).equals(BrowserType.FIREFOX.toString().toLowerCase())
				&& nodeCapabilities.get(CapabilityType.BROWSER_VERSION) != null) {
								
			if (Float.parseFloat((String)nodeCapabilities.get(CapabilityType.BROWSER_VERSION)) < 47.9) {
				requestedCaps.put("marionette", false);
			} else {
				requestedCaps.put("marionette", true);
			}
		}

		// add driver path if it's present in node capabilities, so that they can be transferred to node
		if (nodeCapabilities.get(CapabilityType.BROWSER_NAME) != null) {
			try {
				if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.CHROME.toLowerCase()) && nodeCapabilities.get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY) != null) {
					updateChromeCapabilities(requestedCaps, nodeCapabilities);
				
				} else if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.FIREFOX.toLowerCase()) && nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY) != null) {
					updateFirefoxCapabilities(requestedCaps, nodeCapabilities);
					
				} else if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.IE.toLowerCase()) && nodeCapabilities.get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY) != null) {
					requestedCaps.put(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, nodeCapabilities.get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY).toString());
					nodeClient.setProperty(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, nodeCapabilities.get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY).toString());
				
				} else if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.EDGE.toLowerCase()) && nodeCapabilities.get(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY) != null) {
					requestedCaps.put(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY, nodeCapabilities.get(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY).toString());
					nodeClient.setProperty(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY, nodeCapabilities.get(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY).toString());
				}
			} catch (UnirestException e) {
				throw new ConfigurationException("Could not transfer driver path to node, abord: " + e.getMessage());
			}
		}
		
		// remove se:CONFIG_UUID for IE (issue #15) (moved from CustomDriverProvider)
		if (BrowserType.IE.equals(requestedCaps.get(CapabilityType.BROWSER_NAME))) {
			requestedCaps.remove("se:CONFIG_UUID");
		}
		
		// issue #54: set the platform to family platform, not the more precise one as this fails. Platform value may be useless now as test slot has been selected
		if (!mobilePlatform && requestedCaps.get(CapabilityType.PLATFORM) != null && ((Platform)requestedCaps.get(CapabilityType.PLATFORM)).family() != null) {
			Platform pf = (Platform)requestedCaps.remove(CapabilityType.PLATFORM);
			requestedCaps.put(CapabilityType.PLATFORM, pf.family());
			requestedCaps.remove(CapabilityType.PLATFORM_NAME);
			requestedCaps.put(CapabilityType.PLATFORM_NAME, pf.family().toString());
		}
	}
	
	/**
	 * Update capabilites for firefox, depending on what is requested
	 * @param requestedCaps
	 * @param nodeCapabilities
	 */
	private void updateFirefoxCapabilities(Map<String, Object> requestedCaps, Map<String, Object> nodeCapabilities) {
		requestedCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY).toString());
		

		// in case "firefoxProfile" capability is set, add the '--user-data-dir' option. If value is 'default', search the default user profile
		if (requestedCaps.get("firefoxProfile") != null) {
			try {
				// get some options of the current profile
				FirefoxProfile profile = FirefoxProfile.fromJson((String) requestedCaps.get("firefox_profile"));
				String userAgent = profile.getStringPreference("general.useragent.override", null);
				String ntlmTrustedUris = profile.getStringPreference("network.automatic-ntlm-auth.trusted-uris", null);
				
				FirefoxProfile newProfile;
				if (requestedCaps.get("firefoxProfile").equals(BrowserInfo.DEFAULT_BROWSER_PRODFILE)) {
					newProfile = new ProfilesIni().getProfile("default");
				} else {
					newProfile = new FirefoxProfile(new File((String) requestedCaps.get("firefoxProfile")));
				}
				if (userAgent != null) {
					newProfile.setPreference("general.useragent.override", userAgent);
				}
				if (ntlmTrustedUris != null) {
					newProfile.setPreference("network.automatic-ntlm-auth.trusted-uris", ntlmTrustedUris);
				}
				newProfile.setPreference("capability.policy.default.Window.QueryInterface", ALL_ACCESS);
				newProfile.setPreference("capability.policy.default.Window.frameElement.get", ALL_ACCESS);
				newProfile.setPreference("capability.policy.default.HTMLDocument.compatMode.get", ALL_ACCESS);
				newProfile.setPreference("capability.policy.default.Document.compatMode.get", ALL_ACCESS);
				newProfile.setPreference("dom.max_chrome_script_run_time", 0);
		        newProfile.setPreference("dom.max_script_run_time", 0);
		        requestedCaps.put(FirefoxDriver.PROFILE, newProfile.toJson());
				
			} catch (Exception e) {
				logger.error("Cannot change firefox profile", e);
			}
		}
		
		// issue #60: if "firefox_binary" is set (case of custom / portable browsers), add it to requested caps, else, session is not started
		if (nodeCapabilities.get(FirefoxDriver.BINARY) != null) {
			requestedCaps.put(FirefoxDriver.BINARY, nodeCapabilities.get(FirefoxDriver.BINARY));
		}
		nodeClient.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY).toString());
	
	}
	
	private void updateChromeCapabilities(Map<String, Object> requestedCaps, Map<String, Object> nodeCapabilities) {
		requestedCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, nodeCapabilities.get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).toString());
		
		if (requestedCaps.get(ChromeOptions.CAPABILITY) == null) {
			requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, Object>());
		}
		
		// in case "chromeProfile" capability is set, add the '--user-data-dir' option. If value is 'default', search the default user profile
		if (requestedCaps.get("chromeProfile") != null) {
			if (requestedCaps.get("chromeProfile").equals(BrowserInfo.DEFAULT_BROWSER_PRODFILE)) {
				((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).get("args").add("--user-data-dir=" + nodeCapabilities.get("defaultProfilePath"));
			} else {
				((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).get("args").add("--user-data-dir=" + requestedCaps.get("chromeProfile"));
			}
		}
		
		// issue #60: if "chrome_binary" is set (case of custom / portable browsers), add it to requested caps, else, session is not started
		if (nodeCapabilities.get("chrome_binary") != null ) {
			((Map<String, Object>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("binary", nodeCapabilities.get("chrome_binary"));
		}
		nodeClient.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, nodeCapabilities.get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).toString());
	
	}
	
	/**
	 * Clean node after session
	 * - stop video capture
	 * - stop appium in case of mobile test
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void afterSession(TestSession session) {
		try {
			try {
				nodeClient.stopVideoCapture(session.getExternalKey().getKey());
			} catch (UnirestException | NullPointerException e) {
				
			}
			
			// kill appium. Node will handle the existence of appium itself
			try {
				nodeClient.stopAppium(session.getInternalKey());
			} catch (UnirestException | NullPointerException e) {
				
			}
			
			// kill remaining pids
			if (session.get(PIDS_TO_KILL) != null) {
				for (Long pid: (List<Long>) session.get(PIDS_TO_KILL)) {
					try {
						nodeClient.killProcessByPid(pid);
					} catch (UnirestException e) {
						logger.error(String.format("cannot kill pid %d: %s", pid, e.getMessage()));
					}
				}
			}
		} catch (Exception e) {
			logger.warn("error while terminating session: " + e.getMessage(), e);
		}

		// count session to see if we should restart the node
		disableNodeIfMaxSessionsReached();
		
		// count sessions to see if we should restart the hub
		disableHubIfMaxSessionsReached();

	}
	
	/**
	 * Clean node when no test session is active. It's like a garbage collector when pid killing was not able to remove all drivers / browser processes
	 * Cleaning will
	 * - remove all drivers
	 * - remove all browsers
	 * - clean temp directory as it seems that some browsers write to it
	 */
	public void cleanNode() {
		if (isBusy()) {
			return;
		}
		
		boolean locked = newTestSessionLock.tryLock();
		if (locked) {
			
			try {
				nodeClient.cleanNode();
			} catch (Exception e) {
				logger.warn("error while cleaning node: " + e.getMessage());
			}
			
			// do not crash thread in case the lock has changed between the acquiring and releasing
			// this could happen if cleaning takes more than 'lockTimeout' seconds while a new session is being created. In that case, a new lock is created
			// and releasing this one will lead to an IllegalMonitorStateException
			try {
				newTestSessionLock.unlock();
			} catch (IllegalMonitorStateException e) {
				
			}
		}
	}
	
	/**
	 * Mark node as inactive if a maximum number of sessions has been set and this maximum has been reached
	 */
	public void disableNodeIfMaxSessionsReached() {
		testSessionsCount++;
		Integer maxNodeTestCount = LaunchConfig.getCurrentLaunchConfig().getMaxNodeTestCount();
		if (maxNodeTestCount != null && maxNodeTestCount > 0 && testSessionsCount > maxNodeTestCount) {
			try {
				nodeStatusClient.setStatus(GridStatus.INACTIVE);
			} catch (Exception e) {
				logger.warn(String.format("could not mark node %s as inactive: %s", getRemoteHost().toString(), e.getMessage()));
			}
		}
	}
	
	/**
	 * Mark hub as inactive if
	 * - a max number of session has been set and this maximum has been reached
	 * AND
	 * - hub is not used or max 10% of test slots are used
	 * AND
	 * - this low activity remains for a minute
	 * 
	 * OR if a max number of session has been set and 2 times the maximum has been reached
	 */
	public void disableHubIfMaxSessionsReached() {
		incrementHubTestSessionCount();
		Integer maxHubTestCount = LaunchConfig.getCurrentLaunchConfig().getMaxHubTestCount();
		if (maxHubTestCount != null && maxHubTestCount > 0) {
			
			// if more than 10% of the test slots are in use, we are not in low activity
			// at this stage, the currently finishing session is still count (=> remove it from count)
			double currentActivity = (getUsedTestSlots() - 1) * 1.0 / getHubTotalTestSlots();
			if (currentActivity < 0.1 && getLowActivityBeginning() == null) {
				setLowActivityBeginning();
			} else if (currentActivity >= 0.1) {
				resetLowActivityBeginning();
			}
			
			if (getHubTestSessionCount() > 2 * maxHubTestCount 
					|| getHubTestSessionCount() > maxHubTestCount									// a max number of session has been set and this maximum has been reached
					&& getLowActivityBeginning() != null										// hub is not used or max 10% of test slots are used
					&& getLowActivityBeginning().isBefore(LocalDateTime.now().minusMinutes(1))	// this low activity remains for a minute
					) { 
				setHubStatus(GridStatus.INACTIVE);
			}
		}
	}
	
	/**
	 * Returns the total number of test sessions that can be run behind the hub
	 * Sum of concurrent test sessions for each node
	 * @return
	 */
	public int getHubTotalTestSlots() {
		int testSlotsCount = 0;
		for (RemoteProxy proxy : getRegistry().getAllProxies().getSorted()) {
			testSlotsCount += proxy.getMaxNumberOfConcurrentTestSessions();
		}
		return testSlotsCount;
	}
	
	/**
	 * Returns the number of used slots behind the hub
	 * @return
	 */
	public int getUsedTestSlots() {
		int testSlotsCount = 0;
		for (RemoteProxy proxy : getRegistry().getAllProxies().getSorted()) {
			testSlotsCount += proxy.getTotalUsed();
		}
		return testSlotsCount;
	}
	
	@Override
	public void beforeRelease(TestSession session) {
		// get all pids before session is released in case some processes remain
		beforeStopSession(session);
		
		super.beforeRelease(session);
	}
	
	public boolean isProxyAlive() {
		return super.isAlive();
	}

	@Override
	public boolean isAlive() {
		
		// move mouse to avoid computer session locking (on windows for example)
		try {
			nodeClient.keepAlive();
		} catch (UnirestException e) {
		}
		
		boolean alive = isProxyAlive();
		
		
		if (alive) {
			// clean node
			cleanNode();
			
			// stop node if it's set to inactive, not busy and testSessionCount is greater than max declared
			stopNodeWithMaxSessionsReached();
			
		}
		
		// stop hub if it's set to inactive, not busy and testSessionCount is greater than max declared
		stopHubWithMaxSessionsReached();
		
		return alive;
	}
	
	/**
	 * Stops the node in case max number of session is reached and node is not busy/active
	 */
	public void stopNodeWithMaxSessionsReached() {
		Integer maxNodeTestCount = LaunchConfig.getCurrentLaunchConfig().getMaxNodeTestCount();
		try {
			if (maxNodeTestCount != null 
					&& maxNodeTestCount > 0 
					&& testSessionsCount > maxNodeTestCount
					&& !isBusy()
					&& "inactive".equalsIgnoreCase(nodeStatusClient.getStatus().getString("status"))) {
				nodeClient.stopNode();
			}
		
		} catch (Exception e) {
			if (e instanceof UnirestException && e.getMessage().contains("Connection reset")) {
				return;
			}
			logger.warn(String.format("could not stop node %s: %s", getRemoteHost().toString(), e.getMessage()));
		}
	}
	
	/**
	 * stop hub if it's set to inactive, not busy and testSessionCount is greater than max declared
	 */
	public void stopHubWithMaxSessionsReached() {
		Integer maxHubTestCount = LaunchConfig.getCurrentLaunchConfig().getMaxHubTestCount();
		try {
			if (maxHubTestCount != null 
					&& maxHubTestCount > 0 
					&& getHubTestSessionCount() > maxHubTestCount
					&& getUsedTestSlots() == 0
					&& getHubStatus() == GridStatus.INACTIVE) {
				logger.info("stopping hub");
				getRegistry().getHub().stop();
			}
		} catch (Exception e) {
			logger.warn(String.format("could not stop hub: %s", e.getMessage()));
		}
	}
	
	/**
	 * Returns the status (ACTIVE or INACTIVE) of the hub
	 * @return
	 */
	public synchronized GridStatus getHubStatus() {
		try {
			return GridStatus.fromString(getRegistry().getHub().getConfiguration().custom.get(StatusServlet.STATUS));
		} catch (IllegalArgumentException | NullPointerException e) {
			return null;
		}
	}
	
	public synchronized void setHubStatus(GridStatus status) {
		getRegistry().getHub().getConfiguration().custom.put(StatusServlet.STATUS, status.toString());
	}

	@Override
	public boolean hasCapability(Map<String, Object> requestedCapability) {
		
		GridStatus hubStatus = getHubStatus();
		
		if (hubStatus != null && GridStatus.INACTIVE == hubStatus) {
			logger.info("Node does not accept sessions anymore, waiting to upgrade");
			return false;
		}
		
		// check this node is able to handle new sessions
		try {
			if (GridStatus.INACTIVE.toString().equals(nodeStatusClient.getStatus().get(StatusServlet.STATUS))) {
				return false;
			}
		} catch (JSONException | UnirestException e) {
			return false;
		}
		
		return super.hasCapability(requestedCapability);
	}

	
	public boolean isUpgradeAttempted() {
		return upgradeAttempted;
	}

	public void setUpgradeAttempted(boolean upgradeAttempted) {
		this.upgradeAttempted = upgradeAttempted;
	}
	
	/**
	 * Get list of pids corresponding to our driver, before creating it
	 * This will allow to know the driver pid we have created for this session
	 */
	private void beforeStartSession(TestSession session) {
		try {
			// unlock should occur in "afterCommand", if something goes wrong in the calling method, 'afterCommand' will never be called
			// unlock after 60 secs to avoid deadlocks
			// 60 secs is the delay after which we consider that the driver is created
			boolean locked = newTestSessionLock.tryLock(lockTimeout, TimeUnit.SECONDS);
			
			// timeout reached for the previous lock. We consider that the lock will never be released, so create a new one
			if (!locked) { 
				newTestSessionLock = new ReentrantLock();
				newTestSessionLock.tryLock(lockTimeout, TimeUnit.SECONDS);
			}

			List<Long> existingPids = nodeClient.getDriverPids((String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_NAME), 
														(String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_VERSION),
														new ArrayList<>());
			session.put(PREEXISTING_DRIVER_PIDS, existingPids);
			
		} catch (Exception e) {
			newTestSessionLock.unlock();
		}
	}
	
	/**
	 * Deduce, from the existing pid list for our driver (e.g: chromedriver), the driver we have created
	 */
	private void afterStartSession(TestSession session) {
		// lock should here still be locked
		@SuppressWarnings("unchecked")
		List<Long> existingPids = (List<Long>) session.get(PREEXISTING_DRIVER_PIDS);
		try {
			
			// store the newly created browser/driver pids in the session
			if (existingPids != null) {
				List<Long> browserPid = nodeClient.getDriverPids((String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_NAME), 
						(String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_VERSION),
						existingPids);
				session.put(CURRENT_DRIVER_PIDS, browserPid);
			} else {
				session.put(CURRENT_DRIVER_PIDS, new ArrayList<>());
			}
					
		} catch (UnirestException e) {
			
		} finally {
			if (((ReentrantLock)newTestSessionLock).isLocked()) {
				try {
					newTestSessionLock.unlock();
				} catch (IllegalMonitorStateException e) {}
			}
		}
	}
	
	/**
	 * Before quitting driver, get list of all pids created: driver pid, browser pids and all sub processes created by browser
	 * @param session
	 */
	private void beforeStopSession(TestSession session) {
		try {
			@SuppressWarnings("unchecked")
			List<Long> pidsToKill = nodeClient.getBrowserAndDriverPids((String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_NAME), 
					(String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_VERSION),
					session.get(CURRENT_DRIVER_PIDS) == null ? new ArrayList<>(): (List<Long>) session.get(CURRENT_DRIVER_PIDS));
			session.put(PIDS_TO_KILL, pidsToKill);
		} catch (UnirestException e) {
			logger.error("cannot get list of pids to kill: " + e.getMessage());
		}
	}
	
	/**
	 * Kill all processes identified in beforeStopSession method
	 * @param session
	 */
	@SuppressWarnings("unchecked")
	private void afterStopSession(TestSession session) {
		for (Long pid: (List<Long>) session.get(PIDS_TO_KILL)) {
			try {
				nodeClient.killProcessByPid(pid);
			} catch (UnirestException e) {
				logger.error(String.format("cannot kill pid %d: %s", pid, e.getMessage()));
			}
		}
	}
	

	public synchronized TestSession getNewSession(Map<String, Object> requestedCapability) {
		
		int configuredMaxSessions = config.maxSession;
		
		try {
			if (requestedCapability.containsKey(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE)) {
				// if we are on the right proxy, the one where we want to attach this new session, allow more sessions temporary
				if (requestedCapability.get(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE).equals(remoteHost.toString())) {
					config.maxSession = getTotalUsed() + 1;
					
				// if node do not match, do not go further
				} else {
					return null;
				}
				
			}
			
			return super.getNewSession(requestedCapability);
		} catch (Throwable e) {
			return null;
		} finally {
			config.maxSession = configuredMaxSessions;
		}
	}
	
	@Override
	public TestSlot createTestSlot(SeleniumProtocol protocol, Map<String, Object> capabilities) {
		// create test slot with a copy of this proxy
		RemoteProxy proxyCopy = new CustomRemoteProxyWrapper(this);
		
		String platformName = capabilities.getOrDefault(MobileCapabilityType.PLATFORM_NAME, "nonmobile").toString();
		
		if (platformName.toLowerCase().contains("ios") || platformName.toLowerCase().contains("android")) {
			((CustomRemoteProxyWrapper)proxyCopy).setMobileSlot(true);
		} else {
			((CustomRemoteProxyWrapper)proxyCopy).setMobileSlot(false);
		}

	    return new TestSlot(proxyCopy, protocol, capabilities);
	  }

	public NodeStatusServletClient getNodeStatusClient() {
		return nodeStatusClient;
	}

	public NodeTaskServletClient getNodeClient() {
		return nodeClient;
	}

	public static synchronized Integer getHubTestSessionCount() {
		return CustomRemoteProxy.hubTestSessionCount;
	}

	public static synchronized void incrementHubTestSessionCount() {
		CustomRemoteProxy.hubTestSessionCount++;
	}

	public static synchronized LocalDateTime getLowActivityBeginning() {
		return lowActivityBeginning;
	}

	public static synchronized void resetLowActivityBeginning() {
		CustomRemoteProxy.lowActivityBeginning = null;
	}
	
	public static synchronized void setLowActivityBeginning() {
		CustomRemoteProxy.lowActivityBeginning = LocalDateTime.now();
	}

	public int getTestSessionsCount() {
		return testSessionsCount;
	}

	public void setTestSessionsCount(int testSessionsCount) {
		this.testSessionsCount = testSessionsCount;
	}

	// for test
	public static synchronized void setHubTestSessionCount(Integer hubTestSessionCount) {
		CustomRemoteProxy.hubTestSessionCount = hubTestSessionCount;
	}

	// for test
	public static synchronized void setLowActivityBeginning(LocalDateTime lowActivityBeginning) {
		CustomRemoteProxy.lowActivityBeginning = lowActivityBeginning;
	}
	
	

}
