package com.infotel.seleniumrobot.grid.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import com.infotel.seleniumrobot.grid.utils.CommandLineOptionHelper;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.util.osutility.OSUtility;

public class LaunchConfig {

	public static final String ROLE = "-role";
	public static final String PORT = "-port";
	public static final String BROWSER = "-browser";
	public static final String NODE_CONFIG = "-nodeConfig";
	public static final String HUB_CONFIG = "-hubConfig";
	public static final String DEV_MODE = "-devMode";
	public static final String PROXY_CONFIG = "-proxyConfig"; // if set to "auto", proxy configuration will be reset to this value after each test
	public static final String EXTERNAL_PROGRAMS_WHITE_LIST = "-extProgramWhiteList"; // programs that we will allow to be called from seleniumRobot on this node
	public static final String MAX_NODE_TEST_COUNT = "-maxNodeTestCount"; // max number of test sessions before grid node stops
	public static final String MAX_HUB_TEST_COUNT = "-maxHubTestCount"; // max number of test sessions before grid hub stops
	public static final String NODE_TAGS = "-nodeTags";					// tags / user capabilities that node will present
	public static final String RESTRICT_TO_TAGS = "restrictToTags";	
	public static final String RESTRICT_TO_TAGS_OPTION = "-" + RESTRICT_TO_TAGS;	// test will execute on this node only if one of the tags is requested
	

	private static final List<String> WINDOWS_COMMAND_WHITE_LIST = Arrays.asList("echo", "cmdkey");
	private static final List<String> LINUX_COMMAND_WHITE_LIST = Arrays.asList("echo");
	private static final List<String> MAC_COMMAND_WHITE_LIST = Arrays.asList("echo");
	
	private static LaunchConfig currentLaunchConfig = null;
	
	private List<String> args;
	private String[] originalArgs;
	private Boolean hubRole = null;
	private Boolean devMode = false;
	private Boolean restrictToTags = false;
	private Integer nodePort = null;
	private String configPath = null;
	private Proxy proxyConfig = null;
	private Integer maxNodeTestCount = null;
	private Integer maxHubTestCount = null;
	private List<String> browserConfig = new ArrayList<>();
	private List<String> nodeTags = new ArrayList<>();
	private List<String> externalProgramWhiteList = new ArrayList<>();
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
		if (helper.isParamPresent(NODE_TAGS)) {
			setNodeTags(Arrays.asList(helper.getParamValue(NODE_TAGS).split(",")));
			helper.setArgs(helper.removeAll(NODE_TAGS));
		}
		if (helper.isParamPresent(RESTRICT_TO_TAGS_OPTION)) {
			setRestrictToTags(Boolean.valueOf(helper.getParamValue(RESTRICT_TO_TAGS_OPTION)));
			helper.setArgs(helper.removeAll(RESTRICT_TO_TAGS_OPTION));
		}
		if (helper.isParamPresent(DEV_MODE)) {
			setDevMode(Boolean.valueOf(helper.getParamValue(DEV_MODE)));
			helper.setArgs(helper.removeAll(DEV_MODE));
		}
		if (helper.isParamPresent(EXTERNAL_PROGRAMS_WHITE_LIST)) {
			setExternalProgramWhiteList(Arrays.asList(helper.getParamValue(EXTERNAL_PROGRAMS_WHITE_LIST).split(",")));
			helper.setArgs(helper.removeAll(EXTERNAL_PROGRAMS_WHITE_LIST));
		}
		if (helper.isParamPresent(PROXY_CONFIG)) {
			setProxyConfig(helper.getParamValue(PROXY_CONFIG));
			helper.setArgs(helper.removeAll(PROXY_CONFIG));
		}
		
		// add default white listed programs
		if (OSUtility.isLinux()) {
			externalProgramWhiteList.addAll(LINUX_COMMAND_WHITE_LIST);
		} else if (OSUtility.isWindows()) {
			externalProgramWhiteList.addAll(WINDOWS_COMMAND_WHITE_LIST);
		} else if (OSUtility.isMac()) {
			externalProgramWhiteList.addAll(MAC_COMMAND_WHITE_LIST);
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

	public Proxy getProxyConfig() {
		return proxyConfig;
	}

	public void setProxyConfig(String proxyConfig) {
		Proxy proxy = new Proxy();
		if ("auto".equalsIgnoreCase(proxyConfig)) {
			proxy.setAutodetect(true);
		} else if (proxyConfig.startsWith("pac:")) {
			proxy.setProxyType(ProxyType.PAC);
			proxy.setProxyAutoconfigUrl(proxyConfig.replace("pac:", ""));
		} else if (proxyConfig.equalsIgnoreCase("direct")) {
			proxy.setProxyType(ProxyType.DIRECT);
		} else if (proxyConfig.startsWith("manual:")) {
			String url = proxyConfig.replace("manual:", "");
			proxy.setProxyType(ProxyType.MANUAL);
			proxy.setHttpProxy(url);
		} else {
			throw new ConfigurationException("Only 'auto', 'direct', 'manual:<host>:<port>' and 'pac:<url>' are supported");
		}
		this.proxyConfig = proxy;
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

	public List<String> getNodeTags() {
		return nodeTags;
	}
	
	public void setExternalProgramWhiteList(List<String> externalPrograms) {
		
		this.externalProgramWhiteList.addAll(externalPrograms
				.stream()
				.map(String::trim)
				.collect(Collectors.toList()));
		
	}

	public void setNodeTags(List<String> nodeTags) {
		this.nodeTags = nodeTags
				.stream()
				.map(String::trim)
				.collect(Collectors.toList());
	}

	public Boolean getDevMode() {
		return devMode;
	}

	public void setDevMode(Boolean devMode) {
		this.devMode = devMode;
	}

	public Boolean getRestrictToTags() {
		return restrictToTags;
	}

	public void setRestrictToTags(Boolean restrictToTags) {
		this.restrictToTags = restrictToTags;
	}

	public List<String> getExternalProgramWhiteList() {

		return externalProgramWhiteList;
	}
	
	

}
