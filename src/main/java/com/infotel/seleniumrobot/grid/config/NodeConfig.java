package com.infotel.seleniumrobot.grid.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.infotel.seleniumrobot.grid.config.capability.NodeCapability;
import com.infotel.seleniumrobot.grid.config.configuration.NodeConfiguration;
import com.seleniumtests.customexception.ConfigurationException;

public class NodeConfig {
	
	private NodeConfiguration configuration;
	private List<NodeCapability> capabilities = new ArrayList<>();
	
	
	public static NodeConfig loadFromJson(File jsonConf) throws IOException {
		String jsonString = FileUtils.readFileToString(jsonConf);
		JSONObject conf = new JSONObject(jsonString);
		
		// assume v2 format
		NodeConfig nodeConfig = new NodeConfig();
		try {
			nodeConfig.setConfiguration(NodeConfiguration.fromJson(conf.getJSONObject("configuration")));
			
			for (int i=0; i < conf.getJSONArray("capabilities").length(); i++) {
				nodeConfig.getCapabilities().add(NodeCapability.fromJson(conf.getJSONArray("capabilities").getJSONObject(i)));
			}
		} catch (JSONException e) {
			throw new ConfigurationException("This configuration is not valid. Is it v3 format ?");
		}
		return nodeConfig;
	}
	
	public void toJson(File outputJson) throws IOException {
		String json = new JSONObject(this).toString(4);
		FileUtils.write(outputJson, json);
	}
	
	public NodeConfiguration getConfiguration() {
		return configuration;
	}
	public void setConfiguration(NodeConfiguration configuration) {
		this.configuration = configuration;
	}
	public List<NodeCapability> getCapabilities() {
		return capabilities;
	}
	public void setCapabilities(List<NodeCapability> capabilities) {
		this.capabilities = capabilities;
	}
	
	
}
