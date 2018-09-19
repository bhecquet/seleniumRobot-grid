package com.infotel.seleniumrobot.grid.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;

import com.infotel.seleniumrobot.grid.utils.CommandLineOptionHelper;
import com.seleniumtests.customexception.ConfigurationException;

public class LaunchConfig {

	public static final String ROLE = "-role";
	public static final String PORT = "-port";
	public static final String BROWSER = "-browser";
	public static final String NODE_CONFIG = "-nodeConfig";
	public static final String HUB_CONFIG = "-hubConfig";
	public static final String MAX_NODE_TEST_COUNT = "-maxNodeTestCount"; // max number of test sessions before grid node stops
	public static final String MAX_HUB_TEST_COUNT = "-maxHubTestCount"; // max number of test sessions before grid hub stops
	
	private static LaunchConfig currentLaunchConfig = null;
	
	private List<String> args;
	private String[] originalArgs;
	private Boolean hubRole = null;
	private Integer nodePort = null;
	private String configPath = null;
	private Integer maxNodeTestCount = null;
	private Integer maxHubTestCount = null;
	private List<String> browserConfig = new ArrayList<>();
	private static GridNodeConfiguration currentNodeConfig = null;
	
	public LaunchConfig(String[] args) {
		this.args = Arrays.asList(args);
		originalArgs = args;
		CommandLineOptionHelper helper = new CommandLineOptionHelper(this.args);
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
		if (helper.isParamPresent(PORT)) {
			setNodePort(Integer.valueOf(helper.getParamValue(PORT)));
		}
		if (helper.isParamPresent(MAX_NODE_TEST_COUNT)) {
			setMaxNodeTestCount(Integer.valueOf(helper.getParamValue(MAX_NODE_TEST_COUNT)));
			helper.setArgs(helper.removeAll(MAX_NODE_TEST_COUNT));
		}
		if (helper.isParamPresent(MAX_HUB_TEST_COUNT)) {
			setMaxHubTestCount(Integer.valueOf(helper.getParamValue(MAX_HUB_TEST_COUNT)));
			helper.setArgs(helper.removeAll(MAX_HUB_TEST_COUNT));
		}
		
		if (hubRole == null) {
			throw new ConfigurationException("either hub or node role must be set");
		}
		
		this.args = helper.getAll();

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
		return args.toArray(new String[0]);
	}
	
	public List<String> getArgList() {
		return args;
	}

	public void setArgs(List<String> args) {
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

	public Integer getNodePort() {
		return nodePort;
	}

	public void setNodePort(Integer nodePort) {
		this.nodePort = nodePort;
	}

	public static GridNodeConfiguration getCurrentNodeConfig() {
		return currentNodeConfig;
	}

	public static void setCurrentNodeConfig(GridNodeConfiguration currentNodeConfig) {
		LaunchConfig.currentNodeConfig = currentNodeConfig;
	}

	public Integer getMaxNodeTestCount() {
		return maxNodeTestCount;
	}

	public void setMaxNodeTestCount(Integer maxNodeTestCount) {
		this.maxNodeTestCount = maxNodeTestCount;
	}

	public Integer getMaxHubTestCount() {
		return maxHubTestCount;
	}

	public void setMaxHubTestCount(Integer maxHubTestCount) {
		this.maxHubTestCount = maxHubTestCount;
	}
	
	

}
