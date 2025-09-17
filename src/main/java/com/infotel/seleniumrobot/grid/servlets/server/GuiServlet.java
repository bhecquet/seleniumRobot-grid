package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.client.GridStatusClient;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;
import com.infotel.seleniumrobot.grid.utils.Utils;

import kong.unirest.json.JSONObject;

public class GuiServlet extends GridServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String RESOURCE_LOADER_PATH = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";
	private static final Logger logger = LogManager.getLogger(GuiServlet.class);

	protected VelocityEngine initVelocityEngine() throws Exception {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("resource.loader", "class");
		ve.setProperty("class.resource.loader.class", RESOURCE_LOADER_PATH);
		ve.init();
		return ve;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try (
	        ServletOutputStream outputStream = resp.getOutputStream()) {

			VelocityEngine ve = initVelocityEngine();
			Template t = ve.getTemplate( "templates/hubStatus.vm");
			StringWriter writer = new StringWriter();
			VelocityContext context = new VelocityContext();
			context.put("version", Utils.getCurrentversion());
			
			GridStatusClient gridStatusClient = new GridStatusClient(new URI(String.format("http://%s:%d", 
					LaunchConfig.getCurrentLaunchConfig().getRouterHost(), 
					LaunchConfig.getCurrentLaunchConfig().getRouterPort())));
			

			List<String> activeSessions = new ArrayList<>();
			Map<String, String> nodes = new HashMap<>();
			
			for (SeleniumNode node: gridStatusClient.getNodes()) {
				URL nodeUrl = new URL(node.getExternalUri());
				nodes.put(node.getExternalUri(), node.getExternalUri().replace(Integer.toString(nodeUrl.getPort()), Integer.toString(nodeUrl.getPort() + 10)));
				activeSessions.addAll(node.getSessionList());
			}
			
			context.put("nodes", nodes);
			context.put("activeSessions", activeSessions);

			t.merge( context, writer );
		
			resp.getOutputStream().print(writer.toString());
        } catch (Exception e) {
        	logger.error("Error sending reply", e);
        }
	}


}
