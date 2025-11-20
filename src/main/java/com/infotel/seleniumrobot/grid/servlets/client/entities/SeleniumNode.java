package com.infotel.seleniumrobot.grid.servlets.client.entities;

import kong.unirest.core.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a node, as returned from /status endpoint (only the "node" part)
 * Parse only the node part (no "value" key)
 * <p>
 * {
 * "availability": "UP",
 * "externalUri": "http:\u002f\u002f127.0.0.1:5555",
 * "heartbeatPeriod": 60000,
 * "maxSessions": 3,
 * "nodeId": "48cf9365-2fe6-43ff-b17b-97a7daa63388",
 * "osInfo": {
 * "arch": "amd64",
 * "name": "Windows 10",
 * "version": "10.0"
 * },
 * "slots": [
 * {
 * "id": {
 * "hostId": "48cf9365-2fe6-43ff-b17b-97a7daa63388",
 * "id": "33e23352-d83c-486c-9e98-fa388e875951"
 * },
 * "lastStarted": "1970-01-01T00:00:00Z",
 * "session": null,
 * "stereotype": {
 * "browserName": "chrome",
 * "browserVersion": "106.0",
 * "chrome_binary": "C:\u002fProgram Files\u002fGoogle\u002fChrome Beta\u002fApplication\u002fchrome.exe",
 * "defaultProfilePath": "C:\u002fUsers\u002fS047432\u002fAppData\u002fLocal\u002fGoogle\u002fChrome\u002fUser Data",
 * "max-sessions": 5,
 * "platform": "Windows 10",
 * "platformName": "Windows 10",
 * "sr:restrictToTags": false,
 * "se:webDriverExecutable": "D:\u002fDev\u002fseleniumRobot\u002fseleniumRobot-grid\u002fdrivers\u002fchromedriver_105.0_chrome-105-106.exe",
 * "sr:beta": true,
 * "sr:nodeTags": [
 * "toto"
 * ],
 * "webdriver-executable": "D:\u002fDev\u002fseleniumRobot\u002fseleniumRobot-grid\u002fdrivers\u002fchromedriver_105.0_chrome-105-106.exe"
 * }
 * }
 * ]
 * }
 *
 * @author S047432
 */
public class SeleniumNode {

    private boolean availability;
    private String externalUri;
    private int maxSessions;
    private int testSlots = 0;
    private boolean busy;
    private List<String> sessionList;
    private LocalDateTime lastStarted = null;

    public SeleniumNode(JSONObject status) {
        availability = status.optString("availability", "DOWN").equals("UP");

        externalUri = status.optString("externalUri", null);
        if (externalUri == null) {
            externalUri = status.optString("uri");
        }
        maxSessions = status.getInt("maxSessions");

        sessionList = new ArrayList<>();
        for (JSONObject slot : (List<JSONObject>) status.optJSONArray("slots").toList()) {
            LocalDateTime last = LocalDateTime.parse(slot.getString("lastStarted").replace("Z", ""));
            testSlots += 1;
            if (lastStarted == null || last.isAfter(lastStarted)) {
                lastStarted = last;
            }
            if (slot.optJSONObject("session") != null) {
                sessionList.add(slot.getJSONObject("session").getString("sessionId"));
            }
        }
        busy = !(sessionList.isEmpty());
    }

    public boolean getAvailability() {
        return availability;
    }

    public String getExternalUri() {
        return externalUri;
    }

    public int getMaxSessions() {
        return maxSessions;
    }

    public int getTestSlots() {
        return testSlots;
    }

    public boolean isBusy() {
        return busy;
    }

    public List<String> getSessionList() {
        return sessionList;
    }

    public LocalDateTime getLastStarted() {
        return lastStarted;
    }

}
