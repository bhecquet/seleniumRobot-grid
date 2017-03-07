package com.infotel.seleniumrobot.grid.config;

import java.util.ArrayList;
import java.util.List;

import org.openqa.grid.common.CommandLineOptionHelper;

import com.seleniumtests.customexception.ConfigurationException;

public class LaunchConfig {

	public static final String ROLE = "-role";
	public static final String BROWSER = "-browser";
	public static final String NODE_CONFIG = "-nodeConfig";
	public static final String HUB_CONFIG = "-hubConfig";
	
	private static LaunchConfig currentLaunchConfig = null;
	
	private String[] args;
	private String[] originalArgs;
	private Boolean hubRole = null;
	private String configPath = null;
	private List<String> browserConfig = new ArrayList<>();
	
	public LaunchConfig(String[] args) {
		this.args = args;
		originalArgs = args;
		CommandLineOptionHelper helper = new CommandLineOptionHelper(args);
		if (helper.isParamPresent(ROLE)) {
			setHubRole(helper.getParamValue(ROLE).equals("hub"));			
		}
		if (helper.isParamPresent(HUB_CONFIG)) {
			setConfigPath(helper.getParamValue(HUB_CONFIG));
		}
		if (helper.isParamPresent(NODE_CONFIG)) {
			setConfigPath(helper.getParamValue(NODE_CONFIG));
		}
		if (helper.isParamPresent(BROWSER)) {
			setBrowserConfig(helper.getAll(BROWSER));
		}
		
		if (hubRole == null) {
			throw new ConfigurationException("either hub or node role must be set");
		}
		
		
		
		currentLaunchConfig = this;
	}

	public Boolean getHubRole() {
		return hubRole;
	}

	public void setHubRole(Boolean hubRole) {
		this.hubRole = hubRole;
	}

	public String getConfigPath() {
		return configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	public List<String> getBrowserConfig() {
		return browserConfig;
	}

	public void setBrowserConfig(List<String> browserConfig) {
		this.browserConfig = browserConfig;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public String[] getOriginalArgs() {
		return originalArgs;
	}

	public static LaunchConfig getCurrentLaunchConfig() {
		return currentLaunchConfig;
	}

	public void setOriginalArgs(String[] originalArgs) {
		this.originalArgs = originalArgs;
	}
	
	

}
