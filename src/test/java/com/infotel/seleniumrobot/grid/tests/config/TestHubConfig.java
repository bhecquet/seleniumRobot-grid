package com.infotel.seleniumrobot.grid.tests.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.HubConfig;
import com.infotel.seleniumrobot.grid.config.NodeConfig;
import com.infotel.seleniumrobot.grid.config.configuration.HubConfiguration;

public class TestHubConfig {
	
	protected File createFileFromResource(String resource) throws IOException {
		File tempFile = File.createTempFile("img", null);
		tempFile.deleteOnExit();
		FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource), tempFile);
		
		return tempFile;
	}

	@Test(groups={"grid"})
	public void testToJson() throws IOException {
		HubConfig hubConfig = new HubConfig();
		hubConfig.setConfiguration(new HubConfiguration());
		
		File tempFile = File.createTempFile("conf", ".json");
		hubConfig.toJson(tempFile);
		
		String json = FileUtils.readFileToString(tempFile);
		Assert.assertTrue(json.contains("\"capabilityMatcher\": \"com.infotel.seleniumrobot.grid.CustomCapabilityMatcher\""));
	}
	
	@Test(groups={"grid"})
	public void fromJson() throws IOException {
		File confFile = createFileFromResource("hubConfig.json");
		HubConfig hubConfig = HubConfig.loadFromJson(confFile);
		
		Assert.assertEquals(hubConfig.getConfiguration().getPort(), (Integer)4445);
	}
}
