package com.infotel.seleniumrobot.grid.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.infotel.seleniumrobot.grid.config.configuration.HubConfiguration;

public class HubConfig {
	
	private HubConfiguration configuration;
	
	
	public static HubConfig loadFromJson(File jsonConf) throws IOException {
		String jsonString = FileUtils.readFileToString(jsonConf, Charset.forName("UTF-8"));
		JSONObject conf = new JSONObject(jsonString);
		
		// assume v2 format
		HubConfig hubConfig = new HubConfig();
		hubConfig.setConfiguration(HubConfiguration.fromJson(conf));

		return hubConfig;
	}
	
	public void toJson(File outputJson) throws IOException {
		String json = new JSONObject(this.getConfiguration()).toString(4);
		FileUtils.write(outputJson, json, Charset.forName("UTF-8"));
	}
	
	public HubConfiguration getConfiguration() {
		return configuration;
	}
	public void setConfiguration(HubConfiguration configuration) {
		this.configuration = configuration;
	}
	
}
