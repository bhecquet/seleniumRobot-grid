package com.infotel.seleniumrobot.grid.servlets.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.selenium.grid.web.ServletRequestWrappingHttpRequest;
import org.openqa.selenium.grid.web.ServletResponseWrappingHttpResponse;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;

public class StatusServlet extends GenericServlet {

	private final Json json = new Json();
	public static final String STATUS_RUNNING = "running";
	public static final String STATUS_INACTIVE = "inactive";
	public static final String STATUS = "status";

	public StatusServlet(GridRegistry registry) {
		super(registry);
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

	private void setStatus(HttpRequest request, HttpResponse response) {

		String msg = "OK";
		int statusCode = 200;
		
		String status = request.getQueryParameter("status");

		if (STATUS_RUNNING.equals(status)) {
			getRegistry().getHub().getConfiguration().custom.put(STATUS, STATUS_RUNNING);
		} else if (STATUS_INACTIVE.equals(status)) {
			getRegistry().getHub().getConfiguration().custom.put(STATUS, STATUS_INACTIVE);
		} else {
			msg = "you must provide a 'status' parameter (either 'running' or 'inactive')";
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

	private void sendStatus(HttpRequest request, HttpResponse response) {
		response.setHeader("Content-Type", MediaType.JSON_UTF_8.toString());
		response.setStatus(200);
		try {
			Object res = getResponse(request);
			response.setContent(json.toJson(res).getBytes(UTF_8));
		} catch (Throwable e) {
			throw new GridException(e.getMessage());
		}
	}

	private Map<String, Object> getResponse(HttpRequest request) {
		Map<String, Object> status = new TreeMap<>();
		status.put("success", true);
		status.put("hub", buildHubStatus());

		for (RemoteProxy proxy : getRegistry().getAllProxies().getSorted()) {
			status.put(proxy.getId(), buildNodeStatus(proxy));
		}

		return status;
	}

	private Map<String, String> buildHubStatus() {

		Map<String, String> hubInfos = new HashMap<>();

		String hubStatus = getRegistry().getHub().getConfiguration().custom.get(STATUS);
		if (hubStatus == null) {
			getRegistry().getHub().getConfiguration().custom.put(STATUS, STATUS_RUNNING);
			hubStatus = STATUS_RUNNING;
		}
		hubInfos.put("status", hubStatus);

		return hubInfos;
	}

	private Map<String, Object> buildNodeStatus(RemoteProxy proxy) {

		NodeTaskServletClient nodeClient = new NodeTaskServletClient(proxy.getRemoteHost().getHost(),
				proxy.getRemoteHost().getPort());

		Map<String, Object> nodeInfos = new HashMap<>();
		nodeInfos.put("busy", proxy.isBusy());
		try {
			nodeInfos.put("version", nodeClient.getVersion());
		} catch (IOException | URISyntaxException e) {
			nodeInfos.put("version", "unknown");
		}
		nodeInfos.put("testSlots", proxy.getConfig().maxSession);
		nodeInfos.put("usedTestSlots", proxy.getTotalUsed());
		nodeInfos.put("lastSessionStart", proxy.getLastSessionStart());

		return nodeInfos;
	}

}
