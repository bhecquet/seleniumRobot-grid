package com.infotel.seleniumrobot.grid.tests.config;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.configuration.NodeConfiguration;

public class TestConfiguration {

	@Test(groups={"grid"})
	public void testToJson() {
		NodeConfiguration conf = new NodeConfiguration();
		conf.setHubPort(6666);
		conf.setMaxSession(5);
		conf.setHubHost("127.0.0.1");

		String jsonString = conf.toJson();
		Assert.assertTrue(jsonString.contains("\"hubPort\": 6666"));
		Assert.assertTrue(jsonString.contains("\"maxSessions\": 5"));
		Assert.assertTrue(jsonString.contains("\"hubHost\": \"127.0.0.1\""));
		Assert.assertTrue(jsonString.contains("\"proxy\": \"com.infotel.seleniumrobot.grid.CustomRemoteProxy\""));
	}
	
	@Test(groups={"grid"})
	public void fromJson() {
		String json = "{\"proxy\": \"org.openqa.grid.selenium.proxy.DefaultRemoteProxy\"," +
				    "\"maxSession\": 5," +
				    "\"port\": 6666,"+
				    "\"host\": \"127.0.0.1\"," +
				    "\"register\": true," +
				    "\"registerCycle\": 5000," +
				    "\"hubPort\": 4444," +
				    "\"hubHost\": \"ip\"" +
					"}";
		
		NodeConfiguration conf = NodeConfiguration.fromJson(new JSONObject(json));
		Assert.assertEquals(conf.getPort(), (Integer)6666);
		Assert.assertEquals(conf.getHost(), "127.0.0.1");
	}
}
