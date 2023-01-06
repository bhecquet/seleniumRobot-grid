package com.infotel.seleniumrobot.grid.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.node.config.NodeOptions;
import org.openqa.selenium.grid.server.BaseServerOptions;

import com.google.gson.Gson;

import io.appium.java_client.remote.MobileCapabilityType;

public class GridNodeConfiguration extends GridConfiguration {
	
	
	public static final String WEBDRIVER_PATH = "webdriver-executable";
	public static final String VIDEOS_FOLDER = "videos";
	private Map<String, Map<String, Object>> configuration = new HashMap<>();
	
	public List<MutableCapabilities> capabilities = new ArrayList<>();
	public List<MutableCapabilities> mobileCapabilities = new ArrayList<>(); // for node relay feature
	private BaseServerOptions serverOptions;
	private NodeOptions nodeOptions;
	
	public GridNodeConfiguration() {
		configuration.put("node", new HashMap<>());
		configuration.get("node").put("detect-drivers", false);
	}
	
	private int sessionTimeout = 540;

	/**
	 * https://www.selenium.dev/documentation/grid/configuration/toml_options/
	 * @return
	 */
	public String toToml() {
		
		StringBuilder tomlOut = new StringBuilder();
		
		for (Entry<String, Map<String, Object>> confEntry: configuration.entrySet()) {
			tomlOut.append(String.format("[%s]\n", confEntry.getKey()));
			for (Entry<String, Object> subconfEntry: confEntry.getValue().entrySet()) {
				if (subconfEntry.getValue() instanceof String) {
					tomlOut.append(String.format("%s = \"%s\"\n", subconfEntry.getKey(), subconfEntry.getValue().toString()));
				} else {
					tomlOut.append(String.format("%s = %s\n", subconfEntry.getKey(), subconfEntry.getValue().toString()));
				}
			}

			tomlOut.append("\n");
		}
		tomlOut.append("\n");
		
		for (MutableCapabilities caps: capabilities) {
			tomlOut.append("[[node.driver-configuration]]\n");
			tomlOut.append(String.format("display-name = \"%s %s\"\n", caps.getBrowserName(), caps.getBrowserVersion()));
			tomlOut.append(String.format("webdriver-executable = \"%s\"\n", caps.getCapability(WEBDRIVER_PATH)));
			tomlOut.append(String.format("max-sessions = %d\n", caps.getCapability("max-sessions")));
			tomlOut.append(String.format("stereotype = \"%s\"\n", new Gson().toJson(caps.asMap()).toString().replace("\"", "\\\"")));

			tomlOut.append("\n");
		}
		tomlOut.append("\n");
		
		if (!mobileCapabilities.isEmpty()) {
			tomlOut.append("[relay]\n");
			tomlOut.append("url = \"http://localhost:4723/wd/hub\"\n");
			tomlOut.append("status-endpoint = \"/status\"\n");
			tomlOut.append("configs = [");
			List<String> configs = new ArrayList<>();
			configs.add("\"1\"");
			for (MutableCapabilities caps: mobileCapabilities) {
				Map<String, Object> capsMap = new HashMap<>(caps.asMap());
				if (capsMap.containsKey(MobileCapabilityType.PLATFORM_NAME)) {
					capsMap.put(MobileCapabilityType.PLATFORM_NAME, capsMap.get(MobileCapabilityType.PLATFORM_NAME).toString());
				}
				configs.add(String.format("\"%s\"", new JSONObject(capsMap).toString().replace("\"", "\\\"")));
			}
			tomlOut.append(String.join(",", configs));
			tomlOut.append("]\n");
		}
		
		return tomlOut.toString();
	}
	
	public void addNodeConfiguration(String key, Object value) {
		configuration.get("node").put(key, value);
	}

	public BaseServerOptions getServerOptions() {
		return serverOptions;
	}

	public void setServerOptions(BaseServerOptions serverOptions) {
		this.serverOptions = serverOptions;
	}

	public NodeOptions getNodeOptions() {
		return nodeOptions;
	}

	public void setNodeOptions(NodeOptions nodeOptions) {
		this.nodeOptions = nodeOptions;
	}

	public List<MutableCapabilities> getCapabilities() {
		return capabilities;
	}

}
