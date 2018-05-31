package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import com.infotel.seleniumrobot.grid.utils.Utils;

public class GuiServlet extends RegistryBasedServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String RESOURCE_LOADER_PATH = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";
	private static final Logger logger = Logger.getLogger(GuiServlet.class);

	public GuiServlet() {
	   	this(null);
	}
	
	public GuiServlet(GridRegistry registry) {
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
			Template t = ve.getTemplate( "templates/hubStatus.vm");
			StringWriter writer = new StringWriter();
			VelocityContext context = new VelocityContext();
			context.put("version", Utils.getCurrentversion());
			
			List<RemoteProxy> proxyList = new ArrayList<>();
			List<TestSession> activeSessions = new ArrayList<>();
			
			getRegistry().getAllProxies().forEach(proxyList::add);

			for (RemoteProxy proxy: proxyList) {
				for (TestSlot slot: proxy.getTestSlots()) {
					if (slot.getSession() != null) {
						activeSessions.add(slot.getSession());
					}
				}
			}
			context.put("nodes", proxyList);
			context.put("activeSessions", activeSessions);
			context.put("hubConfiguration", getRegistry().getConfiguration().toJson());
			
			
			
			t.merge( context, writer );
		
			resp.getOutputStream().print(writer.toString());
        } catch (Exception e) {
        	logger.error("Error sending reply", e);
        }
	}


}
