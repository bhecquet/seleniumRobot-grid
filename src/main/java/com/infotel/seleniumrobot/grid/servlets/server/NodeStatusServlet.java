package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.ScreenshotTask;
import com.infotel.seleniumrobot.grid.utils.SystemInfos;
import com.infotel.seleniumrobot.grid.utils.Utils;

public class NodeStatusServlet extends RegistryBasedServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String RESOURCE_LOADER_PATH = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";
	private static final Logger logger = Logger.getLogger(NodeStatusServlet.class);

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
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try (
	        ServletOutputStream outputStream = resp.getOutputStream()) {
			
			VelocityEngine ve = initVelocityEngine();
			Template t = ve.getTemplate( "templates/nodeStatus.vm");
			StringWriter writer = new StringWriter();
			VelocityContext context = new VelocityContext();
			context.put("version", Utils.getCurrentversion());
			context.put("memory", SystemInfos.getMemory());
			context.put("cpu", SystemInfos.getCpuLoad());
			String ip = LaunchConfig.getCurrentNodeConfig().getHubHost();
			context.put("ip", ip.equals("ip") ? "localhost": ip);
			context.put("port", LaunchConfig.getCurrentNodeConfig().getHubPort());
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
