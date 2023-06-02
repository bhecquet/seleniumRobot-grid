package com.infotel.seleniumrobot.grid.servlets.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.json.Json;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.GridStatusClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNodeStatus;
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
	private static final Logger logger = LogManager.getLogger(StatusServlet.class.getName());
	
	public StatusServlet() throws MalformedURLException {
		this(new GridStatusClient(new URL(String.format("http://%s:%d", LaunchConfig.getCurrentLaunchConfig().getRouterHost(), LaunchConfig.getCurrentLaunchConfig().getRouterPort()))));
	}

	public StatusServlet(GridStatusClient gridStatusClient) {
		super();
		
		this.gridStatusClient = gridStatusClient; 
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
				new NodeStatusServletClient(nodeUrl.getHost(), nodeUrl.getPort()).setStatus(status);
			}
			sendOkJson(response, msg);
			return;
			
		} catch (IllegalArgumentException e) {
			msg = "you must provide a 'status' parameter (either 'active' or 'inactive')";
			statusCode = 500;
		} catch (UnirestException | SeleniumGridException | MalformedURLException e) {
			msg = String.format("Error while forwarding status to node: %s", e.getMessage());
			statusCode = 500;
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
				status.put(node.getExternalUri(), buildNodeStatus(nodeUrl));
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

	/**
	 * Build node status from RemoteProxy information and direct information from node itself
	 * @param proxy
	 * @return
	 */
	private Map<String, Object> buildNodeStatus(URL nodeUrl) {

		NodeStatusServletClient nodeStatusServletClient = new NodeStatusServletClient(nodeUrl);
		SeleniumRobotNode status = nodeStatusServletClient.getStatus();
		SeleniumNodeStatus nodeClientStatus = new NodeClient(nodeUrl).getStatus();
		
		Map<String, Object> nodeInfos = new HashMap<>();
		nodeInfos.put("busy", nodeClientStatus.isBusy());
		nodeInfos.put("version", status.getVersion());
		nodeInfos.put("driverVersion", status.getDriverVersion());
		nodeInfos.put("testSlots", status.getMaxSessions());
		nodeInfos.put("usedTestSlots", nodeClientStatus.getSessionList().size());
		nodeInfos.put("lastSessionStart", nodeClientStatus.getLastStarted().format(DateTimeFormatter.ISO_DATE_TIME));
		nodeInfos.put("status", status.getNodeStatus());

		return nodeInfos;
	}

}