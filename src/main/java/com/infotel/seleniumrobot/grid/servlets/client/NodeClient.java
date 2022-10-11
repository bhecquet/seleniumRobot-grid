package com.infotel.seleniumrobot.grid.servlets.client;

import java.net.URI;
import java.net.URL;
import java.util.List;

import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNodeStatus;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

/**
 * Class allowing to perform actions on a node as defined here https://www.selenium.dev/documentation/grid/advanced_features/endpoints/#drain-node
 */
public class NodeClient {
	
	private String nodeUrl;

	/**
	 * The node  URL, typically http://localhost:55555
	 * @param nodeUrl
	 */
	public NodeClient(URL nodeUrl) {
		this.nodeUrl = nodeUrl.toString();
	}
	public NodeClient(URI nodeUrl) {
		this.nodeUrl = nodeUrl.toString();
	}
	
	public void drainNode() {
		
		HttpResponse response = Unirest.post(nodeUrl + "/se/grid/node/drain").header("X-REGISTRATION-SECRET", "").asEmpty();
		if (!response.isSuccess()) {
			throw new SeleniumGridException("Could not drain node");
		}
	}
	
	/**
	 * Parse status reply
	 * 
	 * {
  "value": {
    "ready": true,
    "message": "Ready",
    "node": {
      "availability": "UP",
      "externalUri": "http:\u002f\u002f127.0.0.1:5555",
      "heartbeatPeriod": 60000,
      "maxSessions": 3,
      "nodeId": "48cf9365-2fe6-43ff-b17b-97a7daa63388",
      "osInfo": {
        "arch": "amd64",
        "name": "Windows 10",
        "version": "10.0"
      },
      "slots": [
        {
          "id": {
            "hostId": "48cf9365-2fe6-43ff-b17b-97a7daa63388",
            "id": "33e23352-d83c-486c-9e98-fa388e875951"
          },
          "lastStarted": "1970-01-01T00:00:00Z",
          "session": null,
          "stereotype": {
            "browserName": "chrome",
            "browserVersion": "106.0",
            "chrome_binary": "C:\u002fProgram Files\u002fGoogle\u002fChrome Beta\u002fApplication\u002fchrome.exe",
            "defaultProfilePath": "C:\u002fUsers\u002fS047432\u002fAppData\u002fLocal\u002fGoogle\u002fChrome\u002fUser Data",
            "max-sessions": 5,
            "platform": "Windows 10",
            "platformName": "Windows 10",
            "restrictToTags": false,
            "se:webDriverExecutable": "D:\u002fDev\u002fseleniumRobot\u002fseleniumRobot-grid\u002fdrivers\u002fchromedriver_105.0_chrome-105-106.exe",
            "sr:beta": true,
            "sr:nodeTags": [
              "toto"
            ],
            "webdriver-executable": "D:\u002fDev\u002fseleniumRobot\u002fseleniumRobot-grid\u002fdrivers\u002fchromedriver_105.0_chrome-105-106.exe"
          }
        }
      ],
      "version": "4.2.2 (revision 683ccb65d6)"
    }
  }
}
	 * @return
	 */
	public SeleniumNodeStatus getStatus() {
		return new SeleniumNodeStatus(Unirest.get(nodeUrl + "/status").asJson().getBody().getObject());
	}
	

	public boolean isReady() {
		return getStatus().isReady();
	}
	
	/**
	 * Check whether the node has one of its slots with an active session
	 * @return
	 */
	public boolean isBusy() {
		return getStatus().isBusy();
	}
	
	/**
	 * Check whether the node has one of its slots with an active session
	 * @param sessionIdToExclude	the sessuib ti exclude from search
	 * @return
	 */
	public boolean isBusyOnOtherSlot(String sessionIdToExclude) {
		List<String> sessionList = getStatus().getSessionList();
		sessionList.remove(sessionIdToExclude);
		return !(sessionList.isEmpty());

	}
	
	
}
