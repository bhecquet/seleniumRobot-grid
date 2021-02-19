package com.infotel.seleniumrobot.grid.servlets.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.server.ServletRequestWrappingHttpRequest;
import org.openqa.selenium.grid.server.ServletResponseWrappingHttpResponse;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import com.google.common.base.Functions;
import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.ScreenshotTask;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.infotel.seleniumrobot.grid.utils.MemoryInfo;
import com.infotel.seleniumrobot.grid.utils.SystemInfos;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.PackageUtility;

public class NodeStatusServlet extends GenericServlet {
	
	private final Json json = new Json();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String RESOURCE_LOADER_PATH = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";
	private static final Logger logger = Logger.getLogger(NodeStatusServlet.class);

	private static final String driverVersion = PackageUtility.getDriverVersion();
	
	public NodeStatusServlet() {
	   	this(null);
	}
	
	public NodeStatusServlet(GridRegistry registry) {
		super(registry);
	}
	
	protected VelocityEngine initVelocityEngine() throws Exception {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("resource.loader", "class");
		ve.setProperty("class.resource.loader.class", RESOURCE_LOADER_PATH);
		ve.init();
		return ve;
	}
	
	/**
	 * GET `/extra/NodeStatusServlet`: returns a partial GUI which is used by hub GuiServlet
	 * GET `/extra/NodeStatusServlet&format=json`: returns the node information in json format
	 * {
	 * "memory": {
	 *   "totalMemory": 17054,
	 *   "class": "com.infotel.seleniumrobot.grid.utils.MemoryInfo",
	 *   "freeMemory": 4629
	 * },
	 * "maxSessions": 1,
	 * "port": 5554,
	 * "ip": "node_machine",
	 * "cpu": 25.2,
	 * "version": "3.14.0-SNAPSHOT",
	 * "status": "ACTIVE"
	 * }
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpRequest request = new ServletRequestWrappingHttpRequest(req);
		String format = request.getQueryParameter("format");
		
		if ("json".equalsIgnoreCase(format)) {
			sendJsonStatus(new ServletResponseWrappingHttpResponse(resp));
		} else {
			sendHtmlStatus(resp);
		}
	}

	/**
	 * POST `/extra/NodeStatusServlet?status=INACTIVE`: disable this node. 
	 * It won't accept any new session, but current test session will continue. Allowed values are 'ACTIVE' and 'INACTIVE'
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		setStatus(new ServletRequestWrappingHttpRequest(request), new ServletResponseWrappingHttpResponse(response));
	}

	private void setStatus(HttpRequest request, HttpResponse response) {

		String msg = "OK";
		int statusCode = 200;
		
		try {
			GridStatus status = GridStatus.fromString(request.getQueryParameter("status"));
	
			if (GridStatus.ACTIVE.equals(status)) {
				LaunchConfig.getCurrentNodeConfig().custom.put(StatusServlet.STATUS, GridStatus.ACTIVE.toString());
			} else if (GridStatus.INACTIVE.equals(status)) {
				LaunchConfig.getCurrentNodeConfig().custom.put(StatusServlet.STATUS, GridStatus.INACTIVE.toString());
			}
		} catch (IllegalArgumentException e) {
			msg = "you must provide a 'status' parameter (either 'active' or 'inactive')";
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
	 * Send JSON status of the node
	 */
	private void sendJsonStatus(HttpResponse response) {
		response.setHeader("Content-Type", MediaType.JSON_UTF_8.toString());
		response.setStatus(200);
		try {
			Object res = buildNodeStatus();
			response.setContent(json.toJson(res).getBytes(UTF_8));
		} catch (Throwable e) {
			throw new GridException(e.getMessage());
		}	
	}
	

	private Map<String, Object> buildNodeStatus() {

		Map<String, Object> nodeInfos = new HashMap<>();
		nodeInfos.put("version", Utils.getCurrentversion());
		nodeInfos.put("driverVersion", driverVersion);
		try {
			nodeInfos.put("memory", SystemInfos.getMemory());
			nodeInfos.put("cpu", SystemInfos.getCpuLoad());
		} catch (MalformedObjectNameException | InstanceNotFoundException | ReflectionException | IntrospectionException e) {
			nodeInfos.put("cpu", 11.11);
			nodeInfos.put("memory", new MemoryInfo(0, 0));
		}
		nodeInfos.put("maxSessions", LaunchConfig.getCurrentNodeConfig().maxSession);
		String ip = LaunchConfig.getCurrentNodeConfig().host;
		nodeInfos.put("ip", "ip".equals(ip) ? "localhost": ip);
		nodeInfos.put("hub", LaunchConfig.getCurrentNodeConfig().hub);
		nodeInfos.put("port", LaunchConfig.getCurrentNodeConfig().port);
		nodeInfos.put("nodeTags", LaunchConfig.getCurrentLaunchConfig().getNodeTags());
		nodeInfos.put("capabilities", LaunchConfig.getCurrentNodeConfig()
													.capabilities
													.stream()
													.map(c -> filterCapabilities(c)).collect(Collectors.toList())
													);
		
		String activityStatus = LaunchConfig.getCurrentNodeConfig().custom.get(StatusServlet.STATUS);
		if (activityStatus == null) {
			LaunchConfig.getCurrentNodeConfig().custom.put(StatusServlet.STATUS, GridStatus.ACTIVE.toString());
		}
		
		nodeInfos.put("status", LaunchConfig.getCurrentNodeConfig().custom.get(StatusServlet.STATUS));
		

		return nodeInfos;
	}
	
	/**
	 * Filter capabilities that we will display
	 * @param nodeCapabilities
	 * @return
	 */
	private MutableCapabilities filterCapabilities(MutableCapabilities nodeCapabilities) {

		List<String> capsToKeep = Arrays.asList(CapabilityType.BROWSER_NAME, CapabilityType.BROWSER_VERSION, CapabilityType.PLATFORM_NAME, "maxInstances");
		
		return new MutableCapabilities(nodeCapabilities.asMap()
					.entrySet()
					.stream()
					.filter(cap -> capsToKeep.contains(cap.getKey()))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}
	
	/**
	 * send HTML status of the node
	 * @param resp
	 */
	private void sendHtmlStatus(HttpServletResponse resp) {
		Map<String, Object> status = buildNodeStatus();
		try (
	        ServletOutputStream outputStream = resp.getOutputStream()) {
			
			VelocityEngine ve = initVelocityEngine();
			Template t = ve.getTemplate( "templates/nodeStatus.vm");
			StringWriter writer = new StringWriter();
			VelocityContext context = new VelocityContext();
			context.put("version", status.get("version"));
			context.put("driverVersion", status.get("driverVersion"));
			context.put("memory", status.get("memory"));
			context.put("cpu", status.get("cpu"));
			context.put("ip", status.get("ip"));
			context.put("port", status.get("port"));
			ScreenshotTask screenshotTask = new ScreenshotTask();
			screenshotTask.execute();
			if (screenshotTask.getScreenshot() != null) {
				context.put("image", screenshotTask.getScreenshot());
			}

			
			t.merge( context, writer );
		
			resp.getOutputStream().print(writer.toString());
        } catch (Exception e) {
        	logger.error("Error sending reply", e);
        }
	}
	
	
}
