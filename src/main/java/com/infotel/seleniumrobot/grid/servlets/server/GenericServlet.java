package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

public class GenericServlet extends RegistryBasedServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String SELENIUM_GRID_ALIVE_HEADER = "SELENIUM_GRID_ALIVE";

	public GenericServlet(GridRegistry registry) {
		super(registry);
	}

	@Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(resp);
    	responseWrapper.addHeader("SELENIUM_GRID_ALIVE", "true");
    }
}
