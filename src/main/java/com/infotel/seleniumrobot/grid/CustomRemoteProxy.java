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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.handler.RequestType;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.grid.web.servlet.handler.WebDriverRequest;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.server.jmx.ManagedService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.infotel.seleniumrobot.grid.servlets.client.FileServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.FileServlet;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.screenshots.VideoRecorder;
import com.seleniumtests.util.osutility.OSUtility;

import io.appium.java_client.remote.MobileCapabilityType;

@ManagedService(description = "Selenium Custom Grid Hub TestSlot")
public class CustomRemoteProxy extends DefaultRemoteProxy {
	
	private static final String PREEXISTING_DRIVER_PIDS = "preexistingDriverPids";
	private static final String CURRENT_DRIVER_PIDS = "currentDriverPids";
	private static final String PIDS_TO_KILL = "pidsToKill";
	
	private boolean doNotAcceptTestSessions = false;
	private boolean	upgradeAttempted = false;
	
	private NodeTaskServletClient nodeClient;
	private FileServletClient fileServletClient;
	private MobileNodeServletClient mobileServletClient;

	private Lock lock;
	
	private static final Logger logger = Logger.getLogger(CustomRemoteProxy.class);

	public CustomRemoteProxy(RegistrationRequest request, GridRegistry registry) {
		super(request, registry);
		nodeClient = new NodeTaskServletClient(getRemoteHost().getHost(), getRemoteHost().getPort());
		fileServletClient = new FileServletClient(getRemoteHost().getHost(), getRemoteHost().getPort());
		mobileServletClient = new MobileNodeServletClient(getRemoteHost().getHost(), getRemoteHost().getPort());
		lock = new ReentrantLock();
	}
	
	public CustomRemoteProxy(RegistrationRequest request, GridRegistry registry, NodeTaskServletClient nodeClient, FileServletClient fileServlet, MobileNodeServletClient mobileServletClient) {
		super(request, registry);
		this.nodeClient = nodeClient;
		this.fileServletClient = fileServlet;
		this.mobileServletClient = mobileServletClient;
		lock = new ReentrantLock();
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
			try {
				// unlock should occur in "afterCommand", if something goes wrong in the calling method, 'afterCommand' will never be called
				// unlock after 60 secs to avoid deadlocks
				// 60 secs is the delay after which we consider that the driver is created
				boolean locked = lock.tryLock(60, TimeUnit.SECONDS);
				if (!locked) {
					lock.unlock();
					lock.tryLock(60, TimeUnit.SECONDS);
				}
				
				List<Long> existingPids = nodeClient.getDriverPids((String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_NAME), 
															(String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_VERSION),
															new ArrayList<>());
				session.put(PREEXISTING_DRIVER_PIDS, existingPids);
				
			} catch (Exception e) {
				lock.unlock();
			}
		}
		
		else if(((SeleniumBasedRequest)request).getRequestType() == RequestType.STOP_SESSION) {
			try {
				List<Long> pidsToKill = nodeClient.getBrowserAndDriverPids((String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_NAME), 
						(String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_VERSION),
						session.get(CURRENT_DRIVER_PIDS) == null ? new ArrayList<>(): (List<Long>) session.get(CURRENT_DRIVER_PIDS));
				session.put(PIDS_TO_KILL, pidsToKill);
			} catch (UnirestException e) {
				logger.error("cannot get list of pids to kill: " + e.getMessage());
			}
		}
	}
	

	@Override
	public void afterCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
		super.afterCommand(session, request, response);
		
		
		if(((SeleniumBasedRequest)request).getRequestType() == RequestType.START_SESSION) {

			// lock should here still be locked
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
				if (((ReentrantLock)lock).isLocked()) {
					lock.unlock();
				}
			}
		}
		
		else if(((SeleniumBasedRequest)request).getRequestType() == RequestType.STOP_SESSION && session.get(PIDS_TO_KILL) != null) {
			for (Long pid: (List<Long>) session.get(PIDS_TO_KILL)) {
				try {
					nodeClient.killProcessByPid(pid);
				} catch (UnirestException e) {
					logger.error(String.format("cannot kill pid %d: %s", pid, e.getMessage()));
				}
			}
		}
	}
	
	@Override
	public void beforeSession(TestSession session) {
		
		// add firefox & chrome binary to caps
		super.beforeSession(session);
		Map<String, Object> requestedCaps = session.getRequestedCapabilities();
		
		// update capabilities for mobile. Mobile tests are identified by the use of 'platformName' capability
		// this will allow to add missing caps, for example when client requests an android device without specifying it precisely
		if (requestedCaps.containsKey(MobileCapabilityType.PLATFORM_NAME)) {
			
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
					requestedCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, nodeCapabilities.get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).toString());
					nodeClient.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, nodeCapabilities.get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).toString());
				} else if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.FIREFOX.toLowerCase()) && nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY) != null) {
					requestedCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY).toString());
					nodeClient.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY).toString());
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
	}
	
	@Override
	public void afterSession(TestSession session) {
		try {
			nodeClient.stopVideoCapture(session.getExternalKey().getKey());
		} catch (UnirestException | NullPointerException e) {
			
		}
		
		// kill appium. Node will handle the existence of appium itself
		try {
			nodeClient.stopAppium(session.getInternalKey());
		} catch (UnirestException | NullPointerException e) {
			
		}
		
	}

	@Override
	public boolean isAlive() {
		
		// get version to check if we should update
		String nodeVersion;
		try {
			nodeVersion = nodeClient.getVersion();
			if (nodeVersion != null && !nodeVersion.equals(Utils.getCurrentversion()) && !upgradeAttempted) {
				
				// prevent from accepting new test sessions
				doNotAcceptTestSessions = true;
				
				// update remote jar and restart once node is not used anymore
				if (getTotalUsed() == 0) {
					uploadUpdatedJar(getRemoteHost().getHost(), getRemoteHost().getPort(), this.getId().hashCode());
					nodeClient.restart();
					doNotAcceptTestSessions = false;
				}
				
				
			}
		} catch (IOException | URISyntaxException e) {}
		
		return super.isAlive();
	}

	@Override
	public boolean hasCapability(Map<String, Object> requestedCapability) {
		
		if (doNotAcceptTestSessions) {
			logger.info("Node does not accept sessions anymore, waiting to upgrade");
			return false;
		}
		
		return super.hasCapability(requestedCapability);
	}
	
	private void uploadUpdatedJar(String host, int port, int nodeId) {
		// TODO: to be corrected since jar does not contain libs anymore
		upgradeAttempted = true;
		return;
		
//		// copy current jar to an other folder
//		File gridJar = Utils.getGridJar();
//		
//		if (gridJar != null) {
//			try {
//				File copyTo = Paths.get(gridJar.getParent(), "upgrade_node_" + nodeId, gridJar.getName()).toFile();
//				FileUtils.copyFile(gridJar, copyTo);
//				
//				fileServletClient.upgrade(copyTo.getParent());
//				
//				WaitHelper.waitForSeconds(3);
//				FileUtils.deleteDirectory(copyTo.getParentFile());
//				
//			} catch (Exception e) {
//				logger.warn("cannot copy upgrade file, node won't be updated");
//			}
//		}
	}

	public boolean isDoNotAcceptTestSessions() {
		return doNotAcceptTestSessions;
	}

	public void setDoNotAcceptTestSessions(boolean doNotAcceptTestSessions) {
		this.doNotAcceptTestSessions = doNotAcceptTestSessions;
	}
	
	public boolean isUpgradeAttempted() {
		return upgradeAttempted;
	}

	public void setUpgradeAttempted(boolean upgradeAttempted) {
		this.upgradeAttempted = upgradeAttempted;
	}

}
