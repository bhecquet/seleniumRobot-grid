package com.infotel.seleniumrobot.grid.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.util.osutility.OSUtility;

public class LaunchConfig {

	public enum Role {
	
		HUB("hub"),
		NODE("node"),
		ROUTER("router"),
		DISTRIBUTOR("distributor"),
		SESSION_QUEUE("sessionqueue"),
		SESSIONS("sessions"),
		EVENT_BUS("event-bus");
		
		private String value;
		
		Role(String value) {
			this.value = value;	
		}
		
		public String getValue() {
			return value;
		}
		
		public static Role fromValue(String value) {
			try {
				return Role.valueOf(value);
			} catch (IllegalArgumentException ex) {
				for (Role role : Role.values()) {
			        if (role.value.equalsIgnoreCase(value)) {
			            return role;
			        }
				}
			    throw new IllegalArgumentException("Unrecognized Role: " + value);
			} catch (NullPointerException e) {
				throw new IllegalArgumentException("No/wrong role provided");
			}
		}
	}

	private static final Logger logger = LogManager.getLogger(LaunchConfig.class);
	
	private static final List<String> COMMON_COMMAND_WHITE_LIST = Arrays.asList("echo", "lighthouse");
	@SuppressWarnings("unchecked")
	private static final List<String> WINDOWS_COMMAND_WHITE_LIST = ListUtils.sum(COMMON_COMMAND_WHITE_LIST, Arrays.asList("cmdkey"));
	private static final List<String> LINUX_COMMAND_WHITE_LIST = COMMON_COMMAND_WHITE_LIST;
	private static final List<String> MAC_COMMAND_WHITE_LIST = COMMON_COMMAND_WHITE_LIST;
	public static final String RESTRICT_TO_TAGS = "sr:restrictToTags";	
	public static final String DEFAULT_PROFILE_PATH = "sr:defaultProfilePath";	
	
	private static LaunchConfig currentLaunchConfig = null;
	
	private List<String> args;
	private RouterConfig routerConfig;
	private NodeConfig nodeConfig;
	private OtherConfig otherConfig;
	private String[] originalArgs;
	private Role role = null;
	private String configPath;

	@Parameters(commandDescription = "other parameters")
	public static class OtherConfig {
	}

	@Parameters(commandDescription = "parameters for router")
	public static class RouterConfig {
		@Parameter(
			    description = "Port to listen on",
			    names = {"-p", "--port"})
		private Integer routerPort;
		
		@Parameter(
			    names = {"--host"},
			    description = "Server IP or hostname: usually determined automatically.")
		private String routerHost;
	}
	
	@Parameters(commandDescription = "parameters for node")
	public static class NodeConfig {

		@Parameter(names = "--port", description= "Listen port of this node")
		private Integer nodePort = null;

		@Parameter(names = "--restrictToTags", arity = 1, description= "test will execute on this node only if one of the tags is requested")
		private Boolean restrictToTags = false;
		
		@Parameter(names = "--nodeTags", description= "tags / user capabilities that node will present (comma separated list)")
		private List<String> nodeTags = new ArrayList<>();

		@Parameter(names = "--extProgramWhiteList", description= "programs that we will allow to be called from seleniumRobot on this node")
		private List<String> externalProgramWhiteList = new ArrayList<>();
		
		@Parameter(names = "--proxyConfig", description= "if set to \"auto\", proxy configuration will be reset to this value after each test")
		private String proxyConfigString = null;

		@Parameter(names = "--devMode", arity = 1, description= "if true, browser won't be closed")
		private Boolean devMode = false;

		@Parameter(names = "--max-sessions", description= "Maximum number of sessions on this node. If set to 1, node will still allow to attach existing browsers")
		private Integer maxSessions = null;
	}
	
	
	private Proxy proxyConfig = null;
	private static GridNodeConfiguration currentNodeConfig = null;
	
	public LaunchConfig(String[] args) {
		originalArgs = args;
		
        nodeConfig = new NodeConfig();
        routerConfig = new RouterConfig();
        otherConfig = new OtherConfig();
        
        JCommander jc = JCommander.newBuilder()
	        .addCommand(Role.HUB.getValue(), routerConfig)
	        .addCommand(Role.NODE.getValue(), nodeConfig)
	        .addCommand(Role.ROUTER.getValue(), routerConfig)
	        .addCommand(Role.SESSIONS.getValue(), otherConfig)
	        .addCommand(Role.DISTRIBUTOR.getValue(), otherConfig)
	        .addCommand(Role.SESSION_QUEUE.getValue(), otherConfig)
	        .addCommand(Role.EVENT_BUS.getValue(), otherConfig)
	        .acceptUnknownOptions(true)
	        .build();
        jc.parse(args);
        
        logger.info(String.format("Starting grid with role '%s'", jc.getParsedCommand()));
        setRole(Role.fromValue(jc.getParsedCommand()));
		
		// add default white listed programs
		if (OSUtility.isLinux()) {
			this.nodeConfig.externalProgramWhiteList.addAll(LINUX_COMMAND_WHITE_LIST);
		} else if (OSUtility.isWindows()) {
			this.nodeConfig.externalProgramWhiteList.addAll(WINDOWS_COMMAND_WHITE_LIST);
		} else if (OSUtility.isMac()) {
			this.nodeConfig.externalProgramWhiteList.addAll(MAC_COMMAND_WHITE_LIST);
		}
		
		if (role == Role.NODE) {
			setProxyConfig(nodeConfig.proxyConfigString);
		}
		
		filterArgs(args, jc);
		currentLaunchConfig = this;
	}
	
	private void filterArgs(String[] args, JCommander jc) {
		List<String> unknownArgs = new ArrayList<>();
		unknownArgs.add(jc.getParsedCommand()); // restore the type of command used
		switch(role) {
			case HUB:
			case ROUTER:
				
				// restore some parameters that are consumed by this configuration and also selenium
				if (routerConfig.routerHost != null) {
					unknownArgs.add("--host");
					unknownArgs.add(routerConfig.routerHost.toString());
				} else {
					routerConfig.routerHost = "localhost";
				}
				if (routerConfig.routerPort != null) {
					unknownArgs.add("--port");
					unknownArgs.add(routerConfig.routerPort.toString());
				} else {
					routerConfig.routerPort = 4444;
				}
				
				unknownArgs.add("--slot-matcher");
				unknownArgs.add("com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotMatcher");
				unknownArgs.add("--slot-selector");
				unknownArgs.add("com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotSelector");
				unknownArgs.add("--reject-unsupported-caps");
				unknownArgs.add("true");
				
				
				break;

			case NODE:
				
				// restore some parameters that are consumed by this configuration and also selenium
				if (nodeConfig.nodePort != null) {
					unknownArgs.add("--port");
					unknownArgs.add(nodeConfig.nodePort.toString());
				}
				if (nodeConfig.maxSessions != null) {
					unknownArgs.add("--max-sessions");
					// in case max sessions is too low, add more sessions so that attaching to an existing browser can be done
					if (nodeConfig.maxSessions < 3) { 
						unknownArgs.add("3");
					} else {
						unknownArgs.add(nodeConfig.maxSessions.toString());
					}
				}
				
				unknownArgs.add("--node-implementation");
				unknownArgs.add("com.infotel.seleniumrobot.grid.node.SeleniumRobotNodeFactory");

				break;
			default:
				break;
		}

		unknownArgs.addAll(jc.getUnknownOptions());
		unknownArgs.addAll(jc.getCommands().get(jc.getParsedCommand()).getUnknownOptions());

		this.args = unknownArgs;
		
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getConfigPath() {
		return configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
		args.add("--config");
		args.add(configPath);
	}
	
	public void setNodeImplementation() {
		
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
		if (proxyConfig == null) {
			// do nothing
		} else if ("auto".equalsIgnoreCase(proxyConfig)) {
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
		return nodeConfig.nodePort;
	}
	
	public Integer getRouterPort() {
		return routerConfig.routerPort;
	}
	
	public String getRouterHost() {
		return routerConfig.routerHost;
	}

	public static GridNodeConfiguration getCurrentNodeConfig() {
		return currentNodeConfig;
	}

	public static void setCurrentNodeConfig(GridNodeConfiguration currentNodeConfig) {
		LaunchConfig.currentNodeConfig = currentNodeConfig;
	}

	public List<String> getNodeTags() {
		return nodeConfig.nodeTags;
	}
	
	public Boolean getDevMode() {
		return nodeConfig.devMode;
	}

	public Boolean getRestrictToTags() {
		return nodeConfig.restrictToTags;
	}

	public List<String> getExternalProgramWhiteList() {
		return nodeConfig.externalProgramWhiteList;
	}
	
	public Integer getMaxSessions() {
		return nodeConfig.maxSessions;
	}


	
	

}
