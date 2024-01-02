package com.infotel.seleniumrobot.grid.servlets.server;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.infotel.seleniumrobot.grid.servlets.client.INodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClientFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.json.Json;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.GridStatusClient;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import kong.unirest.UnirestException;

public class StatusServlet extends GridServlet {

	private final Json json = new Json();
	private Configuration jsonPathConf;
	public static final String STATUS = "status";
	private GridStatusClient gridStatusClient;
	private NodeStatusServletClientFactory nodeStatusServletClientFactory;
	private static final Logger logger = LogManager.getLogger(StatusServlet.class.getName());
	
	public StatusServlet() throws MalformedURLException {
		this(new GridStatusClient(new URL(String.format("http://%s:%d", LaunchConfig.getCurrentLaunchConfig().getRouterHost(), LaunchConfig.getCurrentLaunchConfig().getRouterPort()))),
				new NodeStatusServletClientFactory());
	}

	public StatusServlet(GridStatusClient gridStatusClient, NodeStatusServletClientFactory nodeStatusServletClientFactory) {
		super();
		
		this.gridStatusClient = gridStatusClient;
		this.nodeStatusServletClientFactory = nodeStatusServletClientFactory;
		jsonPathConf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
	}


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		sendStatus(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		setStatus(request, response);
	}

	/**
	 * Update hub status and all node statuses
	 * @param request
	 * @param response
	 */
	private void setStatus(HttpServletRequest request, HttpServletResponse response) {

		String msg = "OK";
		int statusCode = 200;
		
		try {
			GridStatus status = GridStatus.fromString(request.getParameter("status"));
	
			if (!(GridStatus.ACTIVE.equals(status) || GridStatus.INACTIVE.equals(status))) {
				sendError(500, response, "Status must be 'active' or 'inactive'");
				return;
			}
		 
			for (SeleniumNode node: gridStatusClient.getNodes()) {
				URL nodeUrl = new URL(node.getExternalUri());
				nodeStatusServletClientFactory.createNodeStatusServletClient(nodeUrl.getHost(), nodeUrl.getPort()).setStatus(status);
			}
			sendOkJson(response, msg);
			return;
			
		} catch (IllegalArgumentException e) {
			msg = "you must provide a 'status' parameter (either 'active' or 'inactive')";
			statusCode = 500;
		} catch (UnirestException | SeleniumGridException | MalformedURLException e) {
			msg = String.format("Error while forwarding status to node: %s", e.getMessage());
			statusCode = 500;
		} catch (InvocationTargetException | NoSuchMethodException |InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
			sendError(statusCode, response, msg);
		} catch (Throwable e) {
			throw new SeleniumGridException(e.getMessage());
		}
	}

	/**
	 * returns the hub and node status
	 * @param request
	 * @param response
	 */
	private void sendStatus(HttpServletRequest request, HttpServletResponse response) {

		try {
			Object status = buildStatus(request);
			String reply = json.toJson(status);
			
			// do we return the whole status or just a part of it ?
			String jsonPath = request.getParameter("jsonpath");
			if (jsonPath != null) {
				status = JsonPath.using(jsonPathConf).parse(reply).read(jsonPath);
				reply = status instanceof String ? status.toString() : json.toJson(status);
			} 
			
			sendOkJson(response, reply);
		} catch (Throwable e) {
			logger.error("Error sending status", e);
			throw new SeleniumGridException(e.getMessage());
		}
	}

	private Map<String, Object> buildStatus(HttpServletRequest request) {
		
		
		Map<String, Object> status = new TreeMap<>();
		status.put("success", true);
		status.put("hub", buildHubStatus());

		for (SeleniumNode node: gridStatusClient.getNodes()) {
			
			try {
				URL nodeUrl = new URL(node.getExternalUri());
				
				INodeStatusServletClient nodeStatusServletClient = nodeStatusServletClientFactory.createNodeStatusServletClient(nodeUrl);
				SeleniumRobotNode nodeStatus = nodeStatusServletClient.getStatus();

				Map<String, Object> nodeInfos = new HashMap<>();
				nodeInfos.put("busy", node.isBusy());
				nodeInfos.put("version", nodeStatus.getVersion());
				nodeInfos.put("driverVersion", nodeStatus.getDriverVersion());
				nodeInfos.put("testSlots", nodeStatus.getMaxSessions());
				nodeInfos.put("usedTestSlots", node.getSessionList().size());
				nodeInfos.put("lastSessionStart", node.getLastStarted().format(DateTimeFormatter.ISO_DATE_TIME));
				nodeInfos.put("status", nodeStatus.getNodeStatus());
				
				
				status.put(node.getExternalUri(), nodeInfos);
			} catch (Exception e) {
				continue;
			}
		}
		

		return status;
	}

	/**
	 * Build hub status
	 * @return
	 */
	private Map<String, String> buildHubStatus() {

		Map<String, String> hubInfos = new HashMap<>();

		hubInfos.put("version", Utils.getCurrentversion());

		return hubInfos;
	}

}