package com.infotel.seleniumrobot.grid.tests.servlets;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNodeStatus;

import kong.unirest.json.JSONObject;

public class TestSeleniumNodeStatus {

	@Test(groups={"grid"})
	public void testStatus() {
		SeleniumNodeStatus status = new SeleniumNodeStatus(new JSONObject("{"
				+ "  \"value\": {"
				+ "    \"ready\": true,"
				+ "    \"message\": \"Ready\","
				+ "    \"node\": {"
				+ "      \"availability\": \"UP\","
				+ "      \"externalUri\": \"http:\\u002f\\u002f127.0.0.1:5555\","
				+ "      \"heartbeatPeriod\": 60000,"
				+ "      \"maxSessions\": 3,"
				+ "      \"nodeId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "      \"osInfo\": {"
				+ "        \"arch\": \"amd64\","
				+ "        \"name\": \"Windows 10\","
				+ "        \"version\": \"10.0\""
				+ "      },"
				+ "      \"slots\": ["
				+ "        {"
				+ "          \"id\": {"
				+ "            \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "            \"id\": \"33e23352-d83c-486c-9e98-fa388e875951\""
				+ "          },"
				+ "          \"lastStarted\": \"1970-01-01T00:00:00Z\","
				+ "          \"session\": null,"
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"chrome\","
				+ "            \"browserVersion\": \"106.0\","
				+ "            \"sr:defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fLocal\\u002fGoogle\\u002fChrome\\u002fUser Data\","
				+ "            \"platform\": \"Windows 10\","
				+ "            \"platformName\": \"Windows 10\","
				+ "            \"sr:restrictToTags\": false,"
				+ "            \"se:webDriverExecutable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fchromedriver_105.0_chrome-105-106.exe\","
				+ "            \"sr:beta\": true,"
				+ "            \"sr:nodeTags\": ["
				+ "              \"toto\""
				+ "            ],"
				+ "            \"webdriver-executable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fchromedriver_105.0_chrome-105-106.exe\""
				+ "          }"
				+ "        },"
				+ "        {"
				+ "          \"id\": {"
				+ "            \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "            \"id\": \"360a2e9f-b4b2-4e61-b1fe-732d5fadafe6\""
				+ "          },"
				+ "          \"lastStarted\": \"2022-01-01T00:00:00Z\","
				+ "          \"session\": \"1234\","
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"firefox\","
				+ "            \"browserVersion\": \"103.0\","
				+ "            \"sr:defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fRoaming\\u002fMozilla\\u002fFirefox\\u002fProfiles\\u002fl4n310bo.default\","
				+ "            \"platform\": \"Windows 10\","
				+ "            \"platformName\": \"Windows 10\","
				+ "            \"sr:restrictToTags\": false,"
				+ "            \"se:webDriverExecutable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fgeckodriver.exe\","
				+ "            \"sr:beta\": false,"
				+ "            \"sr:nodeTags\": ["
				+ "              \"toto\""
				+ "            ],"
				+ "            \"webdriver-executable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fgeckodriver.exe\""
				+ "          }"
				+ "        },"
				+ "        {"
				+ "          \"id\": {"
				+ "            \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "            \"id\": \"343fe894-6563-4aaa-a072-a1885c810a45\""
				+ "          },"
				+ "          \"lastStarted\": \"1970-01-01T00:00:00Z\","
				+ "          \"session\": null,"
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"MicrosoftEdge\","
				+ "            \"browserVersion\": \"105.0\","
				+ "            \"platform\": \"Windows 10\","
				+ "            \"platformName\": \"Windows 10\","
				+ "            \"sr:restrictToTags\": false,"
				+ "            \"se:webDriverExecutable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fedgedriver_105.0_edge-105-106.exe\","
				+ "            \"sr:beta\": false,"
				+ "            \"sr:nodeTags\": ["
				+ "              \"toto\""
				+ "            ],"
				+ "            \"webdriver-executable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fedgedriver_105.0_edge-105-106.exe\""
				+ "          }"
				+ "        }"
				+ "      ]"
				+ "    }"
				+ "  }"
				+ "}"));
		
		Assert.assertTrue(status.getAvailability());
		Assert.assertEquals(status.getExternalUri(), "http://127.0.0.1:5555");
		Assert.assertEquals(status.getMaxSessions(), 3);
		Assert.assertEquals(status.getTestSlots(), 3);
		Assert.assertEquals(status.getLastStarted(), LocalDateTime.parse("2022-01-01T00:00:00"));
		Assert.assertEquals(status.getSessionList(), Arrays.asList("1234"));
		Assert.assertTrue(status.isBusy());
		Assert.assertTrue(status.isReady());
	}
}
