package com.infotel.seleniumrobot.grid.tests.servlets;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;

import kong.unirest.json.JSONObject;

public class TestSeleniumNode {

	@Test(groups={"grid"})
	public void testNodeFromNodeStatus() {
		SeleniumNode node = new SeleniumNode(new JSONObject("{"
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
				+ "          \"lastStarted\": \"1970-01-01T00:00:01Z\","
				+ "          \"session\": null,"
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"chrome\","
				+ "            \"browserVersion\": \"106.0\","
				+ "            \"chrome_binary\": \"C:\\u002fProgram Files\\u002fGoogle\\u002fChrome Beta\\u002fApplication\\u002fchrome.exe\","
				+ "            \"defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fLocal\\u002fGoogle\\u002fChrome\\u002fUser Data\","
				+ "            \"max-sessions\": 5,"
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
				+ "        }"
				+ "      ],"
				+ "      \"version\": \"4.2.2 (revision 683ccb65d6)\""
				+ "}"));
		
		Assert.assertTrue(node.getAvailability());
		Assert.assertEquals(node.getExternalUri(), "http://127.0.0.1:5555");
		Assert.assertEquals(node.getMaxSessions(), 3);
		Assert.assertEquals(node.getTestSlots(), 1);
		Assert.assertEquals(node.getLastStarted(), LocalDateTime.parse("1970-01-01T00:00:01"));
		Assert.assertTrue(node.getSessionList().isEmpty());
		Assert.assertFalse(node.isBusy());
	}
	
	@Test(groups={"grid"})
	public void testNodeSessionStarted() {
		SeleniumNode node = new SeleniumNode(new JSONObject("{"
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
				+ "          \"lastStarted\": \"1980-01-01T00:00:01Z\","
				+ "          \"session\": \"12345\","
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"chrome\","
				+ "            \"browserVersion\": \"106.0\","
				+ "            \"chrome_binary\": \"C:\\u002fProgram Files\\u002fGoogle\\u002fChrome Beta\\u002fApplication\\u002fchrome.exe\","
				+ "            \"defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fLocal\\u002fGoogle\\u002fChrome\\u002fUser Data\","
				+ "            \"max-sessions\": 5,"
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
				+ "        }"
				+ "      ],"
				+ "      \"version\": \"4.2.2 (revision 683ccb65d6)\""
				+ "}"));
		
		Assert.assertTrue(node.getAvailability());
		Assert.assertEquals(node.getExternalUri(), "http://127.0.0.1:5555");
		Assert.assertEquals(node.getMaxSessions(), 3);
		Assert.assertEquals(node.getTestSlots(), 1);
		Assert.assertEquals(node.getLastStarted(), LocalDateTime.parse("1980-01-01T00:00:01"));
		Assert.assertEquals(node.getSessionList(), Arrays.asList("12345"));
		Assert.assertTrue(node.isBusy());
	}
	
	/**
	 * Check we get the last started session
	 */
	@Test(groups={"grid"})
	public void testNodeSessionLastStarted() {
		SeleniumNode node = new SeleniumNode(new JSONObject("{"
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
				+ "          \"lastStarted\": \"1990-01-01T00:00:01Z\","
				+ "          \"session\": \"12345\","
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"chrome\","
				+ "            \"browserVersion\": \"106.0\","
				+ "            \"chrome_binary\": \"C:\\u002fProgram Files\\u002fGoogle\\u002fChrome Beta\\u002fApplication\\u002fchrome.exe\","
				+ "            \"defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fLocal\\u002fGoogle\\u002fChrome\\u002fUser Data\","
				+ "            \"max-sessions\": 5,"
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
				+ "            \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63389\","
				+ "            \"id\": \"33e23352-d83c-486c-9e98-fa388e875952\""
				+ "          },"
				+ "          \"lastStarted\": \"1980-01-01T00:00:01Z\","
				+ "          \"session\": \"12345\","
				+ "          \"stereotype\": {"
				+ "            \"browserName\": \"chrome\","
				+ "            \"browserVersion\": \"106.0\","
				+ "            \"chrome_binary\": \"C:\\u002fProgram Files\\u002fGoogle\\u002fChrome Beta\\u002fApplication\\u002fchrome.exe\","
				+ "            \"defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fLocal\\u002fGoogle\\u002fChrome\\u002fUser Data\","
				+ "            \"max-sessions\": 5,"
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
				+ "        }"
				
				+ "      ],"
				+ "      \"version\": \"4.2.2 (revision 683ccb65d6)\""
				+ "}"));
		

		Assert.assertEquals(node.getLastStarted(), LocalDateTime.parse("1990-01-01T00:00:01"));

	}
	
	/**
	 * From Hub, "externalUri" is replaced by "uri"
	 */
	@Test(groups={"grid"})
	public void testNodeFromHubStatus() {
		SeleniumNode node = new SeleniumNode(new JSONObject("{"
				+ "        \"id\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "        \"uri\": \"http:\\u002f\\u002f127.0.0.1:5555\","
				+ "        \"maxSessions\": 4,"
				+ "        \"osInfo\": {"
				+ "          \"arch\": \"amd64\","
				+ "          \"name\": \"Windows 10\","
				+ "          \"version\": \"10.0\""
				+ "        },"
				+ "        \"heartbeatPeriod\": 60000,"
				+ "        \"availability\": \"UP\","
				+ "        \"version\": \"4.2.2 (revision 683ccb65d6)\","
				+ "        \"slots\": ["
				+ "          {"
				+ "            \"id\": {"
				+ "              \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63388\","
				+ "              \"id\": \"be804800-abe5-4bee-9ec6-87b7068b5247\""
				+ "            },"
				+ "            \"lastStarted\": \"1970-01-01T00:00:00Z\","
				+ "            \"session\": null,"
				+ "            \"stereotype\": {"
				+ "              \"browserName\": \"firefox\","
				+ "              \"browserVersion\": \"103.0\","
				+ "              \"defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fRoaming\\u002fMozilla\\u002fFirefox\\u002fProfiles\\u002fl4n310bo.default\","
				+ "              \"firefox_binary\": \"C:\\u002fProgram Files\\u002fMozilla Firefox\\u002ffirefox.exe\","
				+ "              \"max-sessions\": 5,"
				+ "              \"platform\": \"Windows 10\","
				+ "              \"platformName\": \"Windows 10\","
				+ "              \"sr:restrictToTags\": false,"
				+ "              \"se:webDriverExecutable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fgeckodriver.exe\","
				+ "              \"sr:beta\": false,"
				+ "              \"sr:nodeTags\": ["
				+ "                \"toto\""
				+ "              ],"
				+ "              \"webdriver-executable\": \"D:\\u002fDev\\u002fseleniumRobot\\u002fseleniumRobot-grid\\u002fdrivers\\u002fgeckodriver.exe\""
				+ "            }"
				+ "          }"
				+ "     ]"
				+ "}"));
		
		Assert.assertTrue(node.getAvailability());
		Assert.assertEquals(node.getExternalUri(), "http://127.0.0.1:5555");
		Assert.assertEquals(node.getMaxSessions(), 4);
		Assert.assertEquals(node.getTestSlots(), 1);
		Assert.assertEquals(node.getLastStarted(), LocalDateTime.parse("1970-01-01T00:00:00"));
		Assert.assertTrue(node.getSessionList().isEmpty());
		Assert.assertFalse(node.isBusy());
	}
}
