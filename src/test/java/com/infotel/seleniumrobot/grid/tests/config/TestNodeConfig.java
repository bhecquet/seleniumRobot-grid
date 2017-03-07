package com.infotel.seleniumrobot.grid.tests.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.NodeConfig;
import com.infotel.seleniumrobot.grid.config.capability.MobileCapability;
import com.infotel.seleniumrobot.grid.config.capability.NodeCapability;
import com.infotel.seleniumrobot.grid.config.configuration.NodeConfiguration;

public class TestNodeConfig {
	
	protected File createFileFromResource(String resource) throws IOException {
		File tempFile = File.createTempFile("img", null);
		tempFile.deleteOnExit();
		FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource), tempFile);
		
		return tempFile;
	}

	@Test(groups={"grid"})
	public void testToJson() throws IOException {
		NodeConfig nodeConfig = NodeConfig.buildDefaultConfig();
		MobileCapability nodeCap = new MobileCapability();
		nodeCap.setPlatformName("android");
		nodeCap.setBrowserName("chrome");
		nodeCap.setPlatformVersion("6.0");
		nodeCap.setDeviceName("wiko");
		
		nodeConfig.setCapabilities(Arrays.asList(new NodeCapability[] {nodeCap}));
		File tempFile = File.createTempFile("conf", ".json");
		nodeConfig.toJson(tempFile);
		
		String json = FileUtils.readFileToString(tempFile);
		Assert.assertTrue(json.contains("\"deviceName\": \"wiko\""));
	}
	
	@Test(groups={"grid"})
	public void fromJson() throws IOException {
		File confFile = createFileFromResource("nodeConfig.json");
		NodeConfig nodeConfig = NodeConfig.loadFromJson(confFile);
		
		Assert.assertEquals(nodeConfig.getConfiguration().getPort(), (Integer)6666);
		Assert.assertEquals(nodeConfig.getCapabilities().size(), 6);
	}
}
