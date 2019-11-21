package com.infotel.seleniumrobot.grid.servlets.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.selenium.grid.server.ServletRequestWrappingHttpRequest;
import org.openqa.selenium.grid.server.ServletResponseWrappingHttpResponse;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.CustomRemoteProxy;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClient;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.mashape.unirest.http.exceptions.UnirestException;

public class StatusServlet extends GenericServlet {

	private final Json json = new Json();
	private Configuration jsonPathConf;
	public static final String STATUS = "status";

	public StatusServlet(GridRegistry registry) {
		super(registry);
		
		jsonPathConf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
	}

	public StatusServlet() {
		this(null);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		sendStatus(new ServletRequestWrappingHttpRequest(request), new ServletResponseWrappingHttpResponse(response));
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		setStatus(new ServletRequestWrappingHttpRequest(request), new ServletResponseWrappingHttpResponse(response));
	}

	/**
	 * Update hub status and all node statuses
	 * @param request
	 * @param response
	 */
	private void setStatus(HttpRequest request, HttpResponse response) {

		String msg = "OK";
		int statusCode = 200;
		
		try {
			GridStatus status = GridStatus.fromString(request.getQueryParameter("status"));
	
			if (GridStatus.ACTIVE.equals(status) || GridStatus.INACTIVE.equals(status)) {
				getRegistry().getHub().getConfiguration().custom.put(STATUS, status.toString());
			} else {
				throw new IllegalArgumentException();
			}
			
			for (RemoteProxy proxy : getRegistry().getAllProxies().getSorted()) {
				NodeStatusServletClient nodeStatusClient = ((CustomRemoteProxy)proxy).getNodeStatusClient();
				nodeStatusClient.setStatus(status);
			}
			
		} catch (IllegalArgumentException e) {
			msg = "you must provide a 'status' parameter (either 'active' or 'inactive')";
			statusCode = 500;
		} catch (UnirestException | SeleniumGridException e) {
			msg = String.format("Error while forwarding status to node: %s", e.getMessage());
			statusCode = 500;
		}

		response.setHeader("Content-Type", MediaType.JSON_UTF_8.toString());
		response.setStatus(statusCode);
		try {
			response.setContent(msg.getBytes(UTF_8));
		} catch (Throwable e) {
			throw new GridException(e.getMessage());
		}
	}

	/**
	 * returns the hub and node status
	 * @param request
	 * @param response
	 */
	private void sendStatus(HttpRequest request, HttpResponse response) {
		response.setHeader("Content-Type", MediaType.JSON_UTF_8.toString());
		response.setStatus(200);
		try {
			Object status = buildStatus(request);
			String reply = json.toJson(status);
			
			// do we return the whole status or just a part of it ?
			String jsonPath = request.getQueryParameter("jsonpath");
			if (jsonPath != null) {
				status = JsonPath.using(jsonPathConf).parse(reply).read(jsonPath);
				reply = status instanceof String ? status.toString() : json.toJson(status);
			} 
			
			response.setContent(reply.getBytes(UTF_8));
		} catch (Throwable e) {
			throw new GridException(e.getMessage());
		}
	}

	private Map<String, Object> buildStatus(HttpRequest request) {
		
		
		Map<String, Object> status = new TreeMap<>();
		status.put("success", true);
		status.put("hub", buildHubStatus());

		for (RemoteProxy proxy : getRegistry().getAllProxies().getSorted()) {
			try {
				proxy.getProxyStatus(); // do not build status if hub knows there is a connection problem
				status.put(proxy.getId(), buildNodeStatus(proxy));
			} catch (GridException e) {
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

		String hubStatus = getRegistry().getHub().getConfiguration().custom.get(STATUS);
		if (hubStatus == null) {
			getRegistry().getHub().getConfiguration().custom.put(STATUS, GridStatus.ACTIVE.toString());
			hubStatus = GridStatus.ACTIVE.toString();
		}
		hubInfos.put("status", hubStatus);
		hubInfos.put("version", Utils.getCurrentversion());

		return hubInfos;
	}

	/**
	 * Build node status from RemoteProxy information and direct information from node itself
	 * @param proxy
	 * @return
	 */
	private Map<String, Object> buildNodeStatus(RemoteProxy proxy) {

		NodeStatusServletClient nodeStatusClient = ((CustomRemoteProxy)proxy).getNodeStatusClient();

		Map<String, Object> nodeInfos = new HashMap<>();
		nodeInfos.put("busy", proxy.isBusy());
		try {
			nodeInfos.put("version", nodeStatusClient.getStatus().getString("version"));
		} catch (Exception e) {
			nodeInfos.put("version", "unknown");
		}
		nodeInfos.put("testSlots", proxy.getConfig().maxSession);
		nodeInfos.put("usedTestSlots", proxy.getTotalUsed());
		
		long lastSession = proxy.getLastSessionStart();
		String lastSessionDate = "never";
		if (lastSession > 0) {
			Instant instant = Instant.ofEpochSecond(lastSession / 1000);
			lastSessionDate = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).toString();
		}
		
		nodeInfos.put("lastSessionStart", lastSessionDate);
		try {
			nodeInfos.put("status", nodeStatusClient.getStatus().getString("status"));
		} catch (Exception e) {
			nodeInfos.put("status", GridStatus.UNKNOWN);
		}

		return nodeInfos;
	}

}
