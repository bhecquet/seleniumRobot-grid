package com.infotel.seleniumrobot.grid.aspects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.node.local.SessionSlot;
import org.openqa.selenium.io.Zip;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.SessionId;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.node.SeleniumRobotNodeFactory;
import com.infotel.seleniumrobot.grid.servlets.client.NodeClient;
import com.infotel.seleniumrobot.grid.tasks.CleanNodeTask;
import com.infotel.seleniumrobot.grid.tasks.DiscoverBrowserAndDriverPidsTask;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.video.StopVideoCaptureTask;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.customexception.ConfigurationException;

import kong.unirest.UnirestException;

@Aspect
public class SessionSlotActions {

	private static Logger logger = LogManager.getLogger(SeleniumRobotNodeFactory.class);

	public static final String SE_IE_OPTIONS = "se:ieOptions";
	public static final String EDGE_PATH = "edgePath";
	public static final String ALL_ACCESS = "allAccess";
	public static final int DEFAULT_LOCK_TIMEOUT = 30;
	private static Map<SessionId, List<Long>> pidsToKill = Collections.synchronizedMap(new HashMap<>());
	
	// all PIDs corresponding to browser / driver that already exist on the node
	private static Map<SessionId, List<Long>> preexistingBrowserAndDriverPids = Collections.synchronizedMap(new HashMap<>());
	
	// all PIDs corresponding to browser / driver that have been created by the session
	private static Map<SessionId, List<Long>> currentBrowserAndDriverPids = Collections.synchronizedMap(new HashMap<>());

	private Lock newTestSessionLock;
	private int lockTimeout;
	private NodeClient nodeStatusClient;
	
	public SessionSlotActions() {
		this(DEFAULT_LOCK_TIMEOUT, null);
	}
	
	/**
	 * Constructor to be used in tests
	 * @param lockTimeout
	 * @param nodeStatusClient
	 */
	public SessionSlotActions(int lockTimeout, NodeClient nodeStatusClient) {
		this.lockTimeout = lockTimeout;
		newTestSessionLock = new ReentrantLock();
		this.nodeStatusClient = nodeStatusClient;
	}
	
	private NodeClient getNodeStatusClient() {
		if (nodeStatusClient == null) {
			nodeStatusClient = new NodeClient(LaunchConfig.getCurrentNodeConfig().getServerOptions().getExternalUri());
		} 
		return nodeStatusClient;
	}
	
//	@Around("execution(public * org.openqa.selenium.grid.node.local.SessionSlot..* (..)) ")
//	public Object logLocalNode(ProceedingJoinPoint joinPoint) throws Throwable {
//		System.out.println("coucou2: " + joinPoint.getSignature());
//		Object reply = joinPoint.proceed(joinPoint.getArgs());
//		
//		return reply;
//	} 
	
	@Around("execution(public * org.openqa.selenium.grid.node.local.SessionSlot.apply (..)) ")
	public Object onNewSession(ProceedingJoinPoint joinPoint) throws Throwable {
		
		CreateSessionRequest sessionRequest = (CreateSessionRequest) joinPoint.getArgs()[0];
		SessionSlot slot = (SessionSlot)joinPoint.getThis();
		sessionRequest = beforeStartSession(sessionRequest, slot);
		
		try {
			return joinPoint.proceed(new Object[] {sessionRequest});
		} finally {
			try {
				afterStartSession(slot.getSession().getId(), slot);
			} catch (NoSuchSessionException e) {
			}
			
		}
	} 
	
	@Around("execution(public * org.openqa.selenium.grid.node.local.SessionSlot.stop (..)) ")
	public Object onStopSession(ProceedingJoinPoint joinPoint) throws Throwable {

		SessionSlot slot = (SessionSlot)joinPoint.getThis();
		SessionId sessionId;
		try {
			sessionId = slot.getSession().getId();
		} catch (NoSuchSessionException e) {
			sessionId = null;
		}
		
		beforeStopSession(sessionId, slot);
		try {
			return joinPoint.proceed(joinPoint.getArgs());
		} finally {
			afterStopSession(sessionId);
		}
	} 
	
	public CreateSessionRequest beforeStartSession(CreateSessionRequest sessionRequest, SessionSlot slot) {
		
		if (!getNodeStatusClient().isBusyOnOtherSlot(null)) {
			cleanNode();
		}
		
		// Get list of pids corresponding to our driver, before creating it
		// This will allow to know the driver pid we have created for this session
		try {
			// unlock should occur in "afterStartSession", if something goes wrong in the calling method, 'afterStartSession' may never be called
			// unlock after 30 secs to avoid deadlocks
			// 30 secs is the delay after which we consider that the driver is created
			boolean locked = newTestSessionLock.tryLock(lockTimeout, TimeUnit.SECONDS);
			
			// timeout reached for the previous lock. We consider that the lock will never be released, so create a new one
			if (!locked) { 
				newTestSessionLock = new ReentrantLock();
				newTestSessionLock.tryLock(lockTimeout, TimeUnit.SECONDS);
			}
			
			List<Long> existingPids = new DiscoverBrowserAndDriverPidsTask(slot.getStereotype().getBrowserName(), slot.getStereotype().getBrowserVersion())
							.withExistingPids(new ArrayList<>())
							.execute()
							.getProcessPids();

			setPreexistingBrowserAndDriverPids(slot, existingPids);
			
		} catch (Exception e) {
			newTestSessionLock.unlock();
		}
		

		boolean mobilePlatform = false;
		Map<String, Object> requestedCaps = new HashMap<>(sessionRequest.getDesiredCapabilities().asMap());
		Map<String, Object> slotCaps = slot.getStereotype().asMap();
		
		// TODO: do we need to start appium ? we may use a relay instead, with already started appium server
		// update capabilities for mobile. Mobile tests are identified by the use of 'platformName' capability
		// this will allow to add missing caps, for example when client requests an android device without specifying it precisely
//		String platformName = (String)requestedCaps.getOrDefault(MobileCapabilityType.PLATFORM_NAME, "nonmobile");
//		if (platformName.toLowerCase().contains("ios") || platformName.toLowerCase().contains("android")) {
//			mobilePlatform = true;
//			try {
//				DesiredCapabilities caps = mobileServletClient.updateCapabilities(new DesiredCapabilities(requestedCaps));
//				requestedCaps.putAll(caps.asMap());
//			} catch (IOException | URISyntaxException e) {
//			}
//			
//			try {
//				String appiumUrl = nodeClient.startAppium(session.getInternalKey());
//				requestedCaps.put("appiumUrl", appiumUrl);
//			} catch (UnirestException e) {
//				throw new ConfigurationException("Could not start appium: " + e.getMessage());
//			}
//		}

		// TODO: check if file upload works, for mobile
		// replace all capabilities whose value begins with 'file:' by the remote HTTP URL
		// we assume that these files have been previously uploaded on hub and thus available
//		for (Entry<String, Object> entry: session.getRequestedCapabilities().entrySet()) {
//			if (entry.getValue() instanceof String && ((String)entry.getValue()).startsWith(FileServlet.FILE_PREFIX)) {
//				requestedCaps.put(entry.getKey(), String.format("http://%s:%s/grid/admin/FileServlet/%s", 
//																getConfig().getHubHost(), 
//																getConfig().getHubPort(), 
//																((String)entry.getValue()).replace(FileServlet.FILE_PREFIX, "")));
//			}
//		}

		// add driver path if it's present in node capabilities, so that they can be transferred to node
		String browserName = (String)slotCaps.get(CapabilityType.BROWSER_NAME);
		if (browserName != null) {
			try {
				if (browserName.toLowerCase().contains(Browser.CHROME.browserName().toLowerCase())) {
					updateChromeCapabilities(requestedCaps, slotCaps);
				
				} else if (browserName.toLowerCase().contains(Browser.FIREFOX.browserName().toLowerCase())) {
					updateFirefoxCapabilities(requestedCaps, slotCaps);
					
				} else if (browserName.toLowerCase().contains(Browser.IE.browserName().toLowerCase())) {
					updateInternetExplorerCapabilities(requestedCaps, slotCaps);

				} else if (browserName.toLowerCase().contains(Browser.EDGE.browserName().toLowerCase())) {
					updateEdgeCapabilities(requestedCaps, slotCaps);
				}
			} catch (UnirestException e) {
				throw new ConfigurationException("Could not transfer driver path to node, abord: " + e.getMessage());
			}
		}
		
		// issue #54: set the platform to family platform, not the more precise one as this fails. Platform value may be useless now as test slot has been selected
		if (!mobilePlatform && requestedCaps.get(CapabilityType.PLATFORM_NAME) != null && ((Platform)requestedCaps.get(CapabilityType.PLATFORM_NAME)).family() != null) {
			Platform pf = (Platform)requestedCaps.remove(CapabilityType.PLATFORM_NAME);
			requestedCaps.remove(CapabilityType.PLATFORM_NAME);
			requestedCaps.put(CapabilityType.PLATFORM_NAME, pf.family().toString());
		}
		
		return new CreateSessionRequest(sessionRequest.getDownstreamDialects(), new MutableCapabilities(requestedCaps), sessionRequest.getMetadata());
	}

	
	/**
	 * Before quitting driver, get list of all pids created: driver pid, browser pids and all sub processes created by browser
	 * @param session
	 */
	public void beforeStopSession(SessionId sessionId, SessionSlot slot) {
		
		// stop video capture if it has not already been done
		try {
			new StopVideoCaptureTask(sessionId.toString()).execute();
		} catch (Exception e) {
			
		}
		
		// TODO: appium may now be handled outside of seleniumGrid
		// kill appium. Node will handle the existence of appium itself
//		try {
//			nodeClient.stopAppium(session.getInternalKey());
//		} catch (UnirestException | NullPointerException e) {
//			
//		}
		
		try {
			// search all PIDS corresponding to driver and browser, for this session
			@SuppressWarnings("unchecked")
			List<Long> pids = new DiscoverBrowserAndDriverPidsTask(slot.getStereotype().getBrowserName(), slot.getStereotype().getBrowserVersion())
				.withParentsPids(getCurrentBrowserAndDriverPids(sessionId) == null ? new ArrayList<>(): getCurrentBrowserAndDriverPids(sessionId))
				.execute()
				.getProcessPids();

			setPidsToKill(sessionId, pids);
			removeCurrentBrowserPids(sessionId);
		} catch (Exception e) {
			logger.error("cannot get list of pids to kill: " + e.getMessage());
		}
	}

	
	/**
	 * Kill all processes identified in beforeStopSession method
	 * @param session
	 */
	@SuppressWarnings("unchecked")
	public void afterStopSession(SessionId sessionId) {
		if (sessionId == null) {
			return;
		}
		
		for (Long pid: getPidsToKill(sessionId)) {
			try {
				new KillTask().withPid(pid)
					.execute();
			} catch (Exception e) {
				logger.error(String.format("cannot kill pid %d: %s", pid, e.getMessage()));
			}
		}
		
		// avoid keeping PIDs for terminated sessions
		pidsToKill.remove(sessionId);
		
		if (!getNodeStatusClient().isBusyOnOtherSlot(sessionId.toString())) {
			cleanNode();
		}
	}
	

	/**
	 * Get list of pids corresponding to our driver, before creating it
	 * This will allow to know the driver pid we have created for this session
	 */

	
	/**
	 * Deduce, from the existing pid list for our driver (e.g: chromedriver), the driver we have created
	 */
	public void afterStartSession(SessionId sessionId, SessionSlot slot) {
		// lock should here still be locked
		List<Long> existingPids = getPreexistingBrowserAndDriverPids(sessionId);
		try {
			
			// store the newly created browser/driver pids in the session
			if (existingPids != null) {
				List<Long> browserPid = new DiscoverBrowserAndDriverPidsTask(slot.getStereotype().getBrowserName(), slot.getStereotype().getBrowserVersion())
						.withExistingPids(existingPids)
						.execute()
						.getProcessPids();
				setCurrentBrowserAndDriverPids(sessionId, browserPid);
			} else {
				setCurrentBrowserAndDriverPids(sessionId, new ArrayList<>());
			}
					
		} catch (Exception e) {
			
		} finally {
			removePreexistingPidsForSession(sessionId);
			if (((ReentrantLock)newTestSessionLock).isLocked()) {
				try {
					newTestSessionLock.unlock();
				} catch (IllegalMonitorStateException e) {}
			}
		}
	}

	/* Help tests */
	public void removePreexistingPidsForSession(SessionId sessionId) {
		preexistingBrowserAndDriverPids.remove(sessionId);
	}

	public void setCurrentBrowserAndDriverPids(SessionId sessionId, List<Long> browserPid) {
		currentBrowserAndDriverPids.put(sessionId, browserPid);
	}
	
	public List<Long> getCurrentBrowserAndDriverPids(SessionId sessionId) {
		return currentBrowserAndDriverPids.get(sessionId);
	}

	public void removeCurrentBrowserPids(SessionId sessionId) {
		currentBrowserAndDriverPids.remove(sessionId);
	}

	public List<Long> getPreexistingBrowserAndDriverPids(SessionId sessionId) {
		return preexistingBrowserAndDriverPids.get(sessionId);
	}

	public void setPreexistingBrowserAndDriverPids(SessionSlot slot, List<Long> existingPids) {
		preexistingBrowserAndDriverPids.put(slot.getSession().getId(), existingPids);
	}

	public void setPidsToKill(SessionId sessionId, List<Long> pids) {
		pidsToKill.put(sessionId, pids);
	}

	public List<Long> getPidsToKill(SessionId sessionId) {
		return pidsToKill.getOrDefault(sessionId, new ArrayList<>());
	}
	
	/**
	 * @param requestedCaps
	 * @param slotCaps
	 */
	@SuppressWarnings("unchecked")
	private void updateInternetExplorerCapabilities(Map<String, Object> requestedCaps, Map<String, Object> slotCaps) {
//		requestedCaps.put(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, slotCaps.get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY).toString());
//		nodeClient.setProperty(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, slotCaps.get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY).toString());
		
		if (requestedCaps.containsKey(SeleniumRobotCapabilityType.EDGE_IE_MODE) 
				&& (boolean) requestedCaps.get(SeleniumRobotCapabilityType.EDGE_IE_MODE) 
				&& slotCaps.get(EDGE_PATH) != null) {

			// put in both location as Selenium3 does not handle edge chromium properly
			requestedCaps.putIfAbsent(SE_IE_OPTIONS, new HashMap<>());
			((Map<String, Object>) requestedCaps.get(SE_IE_OPTIONS)).put("ie.edgechromium", true);
			requestedCaps.put("ie.edgechromium", true); 
			((Map<String, Object>) requestedCaps.get(SE_IE_OPTIONS)).put("ie.edgepath", slotCaps.get(EDGE_PATH));
		    requestedCaps.put("ie.edgepath", slotCaps.get(EDGE_PATH));
		}
		
		// remove se:CONFIG_UUID for IE (issue #15) (moved from CustomDriverProvider)
		requestedCaps.remove("se:CONFIG_UUID");
		
	}
	
	private String firefoxProfileToJson(FirefoxProfile profile) throws IOException {
	    File file = profile.layoutOnDisk();
	    try {
	      return Zip.zip(file);
	    } finally {
	      profile.clean(file);
	    }
	  }
	
	/**
	 * Update capabilites for firefox, depending on what is requested
	 * @param requestedCaps
	 * @param slotCaps
	 */
	private void updateFirefoxCapabilities(Map<String, Object> requestedCaps, Map<String, Object> slotCaps) {

		// in case "firefoxProfile" capability is set, add the '--user-data-dir' option. If value is 'default', search the default user profile
		if (requestedCaps.get(SeleniumRobotCapabilityType.FIREFOX_PROFILE) != null) {
			try {
				// get some options of the current profile
				FirefoxProfile profile = FirefoxProfile.fromJson((String) ((Map<String, Object>) requestedCaps
						.get(FirefoxOptions.FIREFOX_OPTIONS))
						.get("profile"));
				String userAgent = profile.getStringPreference("general.useragent.override", null);
				String ntlmTrustedUris = profile.getStringPreference("network.automatic-ntlm-auth.trusted-uris", null);
				
				FirefoxProfile newProfile;
				if (requestedCaps.get(SeleniumRobotCapabilityType.FIREFOX_PROFILE).equals(BrowserInfo.DEFAULT_BROWSER_PRODFILE)) {
					newProfile = new ProfilesIni().getProfile("default");
				} else {
					newProfile = new FirefoxProfile(new File((String) requestedCaps.get(SeleniumRobotCapabilityType.FIREFOX_PROFILE)));
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
		        ((Map<String, Object>) requestedCaps
						.get(FirefoxOptions.FIREFOX_OPTIONS))
		        		.put("profile", firefoxProfileToJson(newProfile));
				
			} catch (Exception e) {
				logger.error("Cannot change firefox profile", e);
			}
		}
		
		// issue #60: if "firefox_binary" is set (case of custom / portable browsers), add it to requested caps, else, session is not started
		if (slotCaps.get(FirefoxDriver.Capability.BINARY) != null) {
			requestedCaps.put(FirefoxDriver.Capability.BINARY, slotCaps.get(FirefoxDriver.Capability.BINARY));
		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateChromeCapabilities(Map<String, Object> requestedCaps, Map<String, Object> slotCaps) {
		
		if (requestedCaps.get(ChromeOptions.CAPABILITY) == null) {
			requestedCaps.put(ChromeOptions.CAPABILITY, new HashMap<String, Object>());
		}
		
		// in case "sr:chromeProfile" capability is set, add the '--user-data-dir' option. If value is 'default', search the default user profile
		if (requestedCaps.get(SeleniumRobotCapabilityType.CHROME_PROFILE) != null) {
			if (requestedCaps.get(SeleniumRobotCapabilityType.CHROME_PROFILE).equals(BrowserInfo.DEFAULT_BROWSER_PRODFILE)) {
				((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).get("args").add("--user-data-dir=" + slotCaps.get("defaultProfilePath"));
			} else {
				((Map<String, List<String>>)requestedCaps.get(ChromeOptions.CAPABILITY)).get("args").add("--user-data-dir=" + requestedCaps.get(SeleniumRobotCapabilityType.CHROME_PROFILE));
			}
		}
		
		// issue #60: if "chrome_binary" is set (case of custom / portable browsers), add it to requested caps, else, session is not started
		if (slotCaps.get("chrome_binary") != null ) {
			((Map<String, Object>)requestedCaps.get(ChromeOptions.CAPABILITY)).put("binary", slotCaps.get("chrome_binary"));
		}	
	}
	
	@SuppressWarnings("unchecked")
	private void updateEdgeCapabilities(Map<String, Object> requestedCaps, Map<String, Object> slotCaps) {
	
		if (requestedCaps.get(EdgeOptions.CAPABILITY) == null) {
			requestedCaps.put(EdgeOptions.CAPABILITY, new HashMap<String, Object>());
		}
		
		// in case "edgeProfile" capability is set, add the '--user-data-dir' option. If value is 'default', search the default user profile
		if (requestedCaps.get(SeleniumRobotCapabilityType.EDGE_PROFILE) != null) {
			if (requestedCaps.get(SeleniumRobotCapabilityType.EDGE_PROFILE).equals(BrowserInfo.DEFAULT_BROWSER_PRODFILE)) {
				((Map<String, List<String>>)requestedCaps.get(EdgeOptions.CAPABILITY)).get("args").add("--user-data-dir=" + slotCaps.get("defaultProfilePath"));
			} else {
				((Map<String, List<String>>)requestedCaps.get(EdgeOptions.CAPABILITY)).get("args").add("--user-data-dir=" + requestedCaps.get(SeleniumRobotCapabilityType.EDGE_PROFILE));
			}
		}
		
		if (slotCaps.get("edge_binary") != null) {
			((Map<String, Object>)requestedCaps.get(EdgeOptions.CAPABILITY)).put("binary", slotCaps.get("edge_binary"));
		}
	
	}
	
	/**
	 * Clean node when no test session is active. It's like a garbage collector when pid killing was not able to remove all drivers / browser processes
	 * Cleaning will
	 * - remove all drivers
	 * - remove all browsers
	 * - clean temp directory as it seems that some browsers write to it
	 * Be sure the node is not used anymore with "isBusy" or isBusyOnOtherSlots
	 */
	public void cleanNode() {
		
		boolean locked = newTestSessionLock.tryLock();
		if (locked) {
			
			
			try {
				new CleanNodeTask().execute();
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

	public static Map<SessionId, List<Long>> getPreexistingBrowserAndDriverPids() {
		return preexistingBrowserAndDriverPids;
	}
}
