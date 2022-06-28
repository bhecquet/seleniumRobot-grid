package com.infotel.seleniumrobot.grid.node;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.grid.config.Config;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.config.NodeOptions;
import org.openqa.selenium.grid.node.local.LocalNodeFactory;
import org.openqa.selenium.grid.server.BaseServerOptions;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.server.WebServer;

public class SeleniumRobotNodeFactory extends LocalNodeFactory {
	
	private static Logger logger = LogManager.getLogger(SeleniumRobotNodeFactory.class);
		
	/**
	 * Copied from LocalNodeFactory
	 * @param config
	 * @return
	 */
	public static Node create(Config config) {

	    // store configuration
	    LaunchConfig.getCurrentNodeConfig().setServerOptions(new BaseServerOptions(config));
		LaunchConfig.getCurrentNodeConfig().setNodeOptions(new NodeOptions(config));
		
		// add servlets
		try {
			new WebServer().startNodeServletServer(new BaseServerOptions(config).getPort() + 10);
		} catch (Exception e) {
			throw new SeleniumGridException("Error starting servlet server");
		}
		
		logger.info("Adding servlets");

	    return LocalNodeFactory.create(config);
	  }


}
