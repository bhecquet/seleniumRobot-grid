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
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.zeroturnaround.zip.commons.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.infotel.seleniumrobot.grid.servlets.client.FileServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.MobileNodeServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.FileServlet;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.helper.WaitHelper;

import io.appium.java_client.remote.MobileCapabilityType;

public class CustomRemoteProxy extends DefaultRemoteProxy {
	
	private boolean doNotAcceptTestSessions = false;
	private boolean	upgradeAttempted = false;
	
	private NodeTaskServletClient nodeClient;
	private FileServletClient fileServletClient;
	private MobileNodeServletClient mobileServletClient;
	
	private static final Logger logger = Logger.getLogger(CustomRemoteProxy.class);

	public CustomRemoteProxy(RegistrationRequest request, Registry registry) {
		super(request, registry);
		nodeClient = new NodeTaskServletClient(getRemoteHost().getHost(), getRemoteHost().getPort());
		fileServletClient = new FileServletClient(getRemoteHost().getHost(), getRemoteHost().getPort());
		mobileServletClient = new MobileNodeServletClient(getRemoteHost().getHost(), getRemoteHost().getPort());
	}
	
	public CustomRemoteProxy(RegistrationRequest request, Registry registry, NodeTaskServletClient nodeClient, FileServletClient fileServlet, MobileNodeServletClient mobileServletClient) {
		super(request, registry);
		this.nodeClient = nodeClient;
		this.fileServletClient = fileServlet;
		this.mobileServletClient = mobileServletClient;
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

		} catch (JsonSyntaxException | IllegalStateException  e) {
		}

	  }
	
	@Override
	public void beforeSession(TestSession session) {
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
		

		// add driver path if it's present in node capabilities, so that they can be transferred to node
		Map<String, Object> nodeCapabilities = session.getSlot().getCapabilities();
		if (nodeCapabilities.get(CapabilityType.BROWSER_NAME) != null) {
			if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.CHROME.toLowerCase()) && nodeCapabilities.get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY) != null) {
				requestedCaps.put(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, nodeCapabilities.get(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY).toString());
			} else if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.FIREFOX.toLowerCase()) && nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY) != null) {
				requestedCaps.put(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, nodeCapabilities.get(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY).toString());
			} else if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.IE.toLowerCase()) && nodeCapabilities.get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY) != null) {
				requestedCaps.put(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, nodeCapabilities.get(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY).toString());
			} else if (nodeCapabilities.get(CapabilityType.BROWSER_NAME).toString().toLowerCase().contains(BrowserType.EDGE.toLowerCase()) && nodeCapabilities.get(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY) != null) {
				requestedCaps.put(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY, nodeCapabilities.get(EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY).toString());
			}
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
		
		upgradeAttempted = true;
		
		// copy current jar to an other folder
		File gridJar = Utils.getGridJar();
		
		if (gridJar != null) {
			try {
				File copyTo = Paths.get(gridJar.getParent(), "upgrade_node_" + nodeId, gridJar.getName()).toFile();
				FileUtils.copyFile(gridJar, copyTo);
				
				fileServletClient.upgrade(copyTo.getParent());
				
				WaitHelper.waitForSeconds(3);
				FileUtils.deleteDirectory(copyTo.getParentFile());
				
			} catch (Exception e) {
				logger.warn("cannot copy upgrade file, node won't be updated");
			}
		}
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
