package com.infotel.seleniumrobot.grid.tests.servlets;

import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;
import kong.unirest.core.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

public class TestSeleniumNode {

    @Test(groups = {"grid"})
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


    @Test(groups = {"grid"})
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
                + "          \"session\": {\r\n"
                + "				\"capabilities\": {\r\n"
                + "					\"acceptInsecureCerts\": true,\r\n"
                + "					\"browserName\": \"chrome\",\r\n"
                + "					\"browserVersion\": \"109.0.5414.75\",\r\n"
                + "					\"chrome\": {\r\n"
                + "						\"chromedriverVersion\": \"109.0.5414.74 (e7c5703604daa9cc128ccf5a5d3e993513758913-refs/branch-heads/5414@{#1172})\",\r\n"
                + "						\"userDataDir\": \"C:/Users/a7203a99/AppData/Local/Google/Chrome/User Data\"\r\n"
                + "					},\r\n"
                + "					\"goog:chromeOptions\": {\r\n"
                + "						\"debuggerAddress\": \"localhost:62676\"\r\n"
                + "					},\r\n"
                + "					\"networkConnectionEnabled\": false,\r\n"
                + "					\"pageLoadStrategy\": \"normal\",\r\n"
                + "					\"platformName\": \"Windows 10\",\r\n"
                + "					\"proxy\": {\r\n"
                + "						\"proxyType\": \"autodetect\"\r\n"
                + "					},\r\n"
                + "					\"se:cdp\": \"http://localhost:62676\",\r\n"
                + "					\"se:cdpVersion\": \"109.0.5414.75\",\r\n"
                + "					\"setWindowRect\": true,\r\n"
                + "					\"strictFileInteractability\": false,\r\n"
                + "					\"timeouts\": {\r\n"
                + "						\"implicit\": 0,\r\n"
                + "						\"pageLoad\": 300000,\r\n"
                + "						\"script\": 30000\r\n"
                + "					},\r\n"
                + "					\"unhandledPromptBehavior\": \"dismiss and notify\",\r\n"
                + "					\"webauthn:extension:credBlob\": true,\r\n"
                + "					\"webauthn:extension:largeBlob\": true,\r\n"
                + "					\"webauthn:virtualAuthenticators\": true\r\n"
                + "				},\r\n"
                + "				\"sessionId\": \"12345\",\r\n"
                + "				\"start\": \"2023-06-22T13:07:39.869Z\",\r\n"
                + "				\"stereotype\": {\r\n"
                + "					\"browserName\": \"chrome\",\r\n"
                + "					\"browserVersion\": \"109.0\",\r\n"
                + "					\"goog:chromeOptions\": {\r\n"
                + "						\"binary\": \"C:/Program Files (x86)/Google/Chrome/Application/chrome.exe\"\r\n"
                + "					},\r\n"
                + "					\"platformName\": \"Windows 10\",\r\n"
                + "					\"se:webDriverExecutable\": \"D:/SeleniumGrid/drivers/chromedriver_109.0_chrome-109-110.exe\",\r\n"
                + "					\"sr:beta\": false,\r\n"
                + "					\"sr:defaultProfilePath\": \"C:/Users/a7203a99/AppData/Local/Google/Chrome/User Data\",\r\n"
                + "					\"sr:nodeTags\": [\r\n"
                + "						\"pf21027892.societe.mma.fr\",\r\n"
                + "						\"agent\",\r\n"
                + "						\"prod\"\r\n"
                + "					],\r\n"
                + "					\"sr:restrictToTags\": true\r\n"
                + "				},\r\n"
                + "				\"uri\": \"http://localhost:44122\"\r\n"
                + "			 },"
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
    @Test(groups = {"grid"})
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
                + "          \"session\": {\r\n"
                + "				\"capabilities\": {\r\n"
                + "					\"acceptInsecureCerts\": true,\r\n"
                + "					\"browserName\": \"chrome\",\r\n"
                + "					\"browserVersion\": \"109.0.5414.75\",\r\n"
                + "					\"chrome\": {\r\n"
                + "						\"chromedriverVersion\": \"109.0.5414.74 (e7c5703604daa9cc128ccf5a5d3e993513758913-refs/branch-heads/5414@{#1172})\",\r\n"
                + "						\"userDataDir\": \"C:/Users/a7203a99/AppData/Local/Google/Chrome/User Data\"\r\n"
                + "					},\r\n"
                + "					\"goog:chromeOptions\": {\r\n"
                + "						\"debuggerAddress\": \"localhost:62676\"\r\n"
                + "					},\r\n"
                + "					\"networkConnectionEnabled\": false,\r\n"
                + "					\"pageLoadStrategy\": \"normal\",\r\n"
                + "					\"platformName\": \"Windows 10\",\r\n"
                + "					\"proxy\": {\r\n"
                + "						\"proxyType\": \"autodetect\"\r\n"
                + "					},\r\n"
                + "					\"se:cdp\": \"http://localhost:62676\",\r\n"
                + "					\"se:cdpVersion\": \"109.0.5414.75\",\r\n"
                + "					\"setWindowRect\": true,\r\n"
                + "					\"strictFileInteractability\": false,\r\n"
                + "					\"timeouts\": {\r\n"
                + "						\"implicit\": 0,\r\n"
                + "						\"pageLoad\": 300000,\r\n"
                + "						\"script\": 30000\r\n"
                + "					},\r\n"
                + "					\"unhandledPromptBehavior\": \"dismiss and notify\",\r\n"
                + "					\"webauthn:extension:credBlob\": true,\r\n"
                + "					\"webauthn:extension:largeBlob\": true,\r\n"
                + "					\"webauthn:virtualAuthenticators\": true\r\n"
                + "				},\r\n"
                + "				\"sessionId\": \"12345\",\r\n"
                + "				\"start\": \"2023-06-22T13:07:39.869Z\",\r\n"
                + "				\"stereotype\": {\r\n"
                + "					\"browserName\": \"chrome\",\r\n"
                + "					\"browserVersion\": \"109.0\",\r\n"
                + "					\"goog:chromeOptions\": {\r\n"
                + "						\"binary\": \"C:/Program Files (x86)/Google/Chrome/Application/chrome.exe\"\r\n"
                + "					},\r\n"
                + "					\"platformName\": \"Windows 10\",\r\n"
                + "					\"se:webDriverExecutable\": \"D:/SeleniumGrid/drivers/chromedriver_109.0_chrome-109-110.exe\",\r\n"
                + "					\"sr:beta\": false,\r\n"
                + "					\"sr:defaultProfilePath\": \"C:/Users/a7203a99/AppData/Local/Google/Chrome/User Data\",\r\n"
                + "					\"sr:nodeTags\": [\r\n"
                + "						\"pf21027892.societe.mma.fr\",\r\n"
                + "						\"agent\",\r\n"
                + "						\"prod\"\r\n"
                + "					],\r\n"
                + "					\"sr:restrictToTags\": true\r\n"
                + "				},\r\n"
                + "				\"uri\": \"http://localhost:44122\"\r\n"
                + "			 },"
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
                + "            \"hostId\": \"48cf9365-2fe6-43ff-b17b-97a7daa63389\","
                + "            \"id\": \"33e23352-d83c-486c-9e98-fa388e875952\""
                + "          },"
                + "          \"lastStarted\": \"1980-01-01T00:00:01Z\","
                + "          \"session\": \"12345\","
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
                + "        }"

                + "      ],"
                + "      \"version\": \"4.2.2 (revision 683ccb65d6)\""
                + "}"));


        Assert.assertEquals(node.getLastStarted(), LocalDateTime.parse("1990-01-01T00:00:01"));

    }

    /**
     * From Hub, "externalUri" is replaced by "uri"
     */
    @Test(groups = {"grid"})
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
                + "              \"sr:defaultProfilePath\": \"C:\\u002fUsers\\u002fS047432\\u002fAppData\\u002fRoaming\\u002fMozilla\\u002fFirefox\\u002fProfiles\\u002fl4n310bo.default\","
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
