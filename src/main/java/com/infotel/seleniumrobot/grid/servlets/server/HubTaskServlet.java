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
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;

import com.infotel.seleniumrobot.grid.utils.Utils;

public class HubTaskServlet extends GenericServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String RESOURCE_LOADER_PATH = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";
	private static final Logger logger = Logger.getLogger(HubTaskServlet.class);

	public HubTaskServlet() {
	   	this(null);
	}
	
	public HubTaskServlet(GridRegistry registry) {
		super(registry);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		switch (req.getParameter("action")) {
		case "ignoreTimeout":
			String session = req.getParameter("session");
			boolean ignore = "true".equalsIgnoreCase(req.getParameter("ignore")) ? true: false; // "true" or "false"
			setIgnoreTimeout(ignore, session);
			break;
		}
	}
	
	private void setIgnoreTimeout(boolean ignoreTimeout, String sessionKey) {
		TestSession testSession = getRegistry().getExistingSession(new ExternalSessionKey(sessionKey));
		testSession.setIgnoreTimeout(ignoreTimeout);
	}

}
