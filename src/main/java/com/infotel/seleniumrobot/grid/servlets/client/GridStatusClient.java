package com.infotel.seleniumrobot.grid.servlets.client;

import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class allowing to get info from "/status" of grid
 */
public class GridStatusClient {

    private String gridUrl;

    /**
     * The grid router URL, typically http://localhost:4444
     *
     * @param gridUrl
     */
    public GridStatusClient(URL gridUrl) {
        this.gridUrl = gridUrl.toString() + "/status";
    }

    public GridStatusClient(URI gridUrl) {
        this.gridUrl = gridUrl.toString() + "/status";
    }

    /**
     * Parse status response
     * {
     * "value": {
     * "ready": true,
     * "message": "Selenium Grid ready.",
     * "nodes": [
     * {
     * "id": "48cf9365-2fe6-43ff-b17b-97a7daa63388",
     * "uri": "http:\u002f\u002f127.0.0.1:5555",
     * "maxSessions": 3,
     * "osInfo": {
     * "arch": "amd64",
     * "name": "Windows 10",
     * "version": "10.0"
     * },
     * "heartbeatPeriod": 60000,
     * "availability": "UP",
     * "version": "4.2.2 (revision 683ccb65d6)",
     * "slots": [
     * {
     * "id": {
     * "hostId": "48cf9365-2fe6-43ff-b17b-97a7daa63388",
     * "id": "be804800-abe5-4bee-9ec6-87b7068b5247"
     * },
     * "lastStarted": "1970-01-01T00:00:00Z",
     * "session": null,
     * "stereotype": {
     * "browserName": "firefox",
     * "browserVersion": "103.0",
     * "defaultProfilePath": "C:\u002fUsers\u002fS047432\u002fAppData\u002fRoaming\u002fMozilla\u002fFirefox\u002fProfiles\u002fl4n310bo.default",
     * "firefox_binary": "C:\u002fProgram Files\u002fMozilla Firefox\u002ffirefox.exe",
     * "max-sessions": 5,
     * "platform": "Windows 10",
     * "platformName": "Windows 10",
     * "sr:restrictToTags": false,
     * "se:webDriverExecutable": "D:\u002fDev\u002fseleniumRobot\u002fseleniumRobot-grid\u002fdrivers\u002fgeckodriver.exe",
     * "sr:beta": false,
     * "sr:nodeTags": [
     * "toto"
     * ],
     * "webdriver-executable": "D:\u002fDev\u002fseleniumRobot\u002fseleniumRobot-grid\u002fdrivers\u002fgeckodriver.exe"
     * }
     * }
     * ]
     * }
     * ]
     * }
     * }
     *
     * @return
     */
    public JSONObject getStatus() {

        JsonNode status = Unirest.get(gridUrl).asJson().getBody();
        return status.getObject().getJSONObject("value");
    }

    public boolean isReady() {
        return getStatus().optBoolean("ready", false);
    }

    public List<SeleniumNode> getNodes() {
        List<SeleniumNode> nodeList = new ArrayList<>();

        for (JSONObject nodeJson : (List<JSONObject>) getStatus().optJSONArray("nodes").toList()) {
            SeleniumNode node = new SeleniumNode(nodeJson);
            nodeList.add(node);
        }

        return nodeList;
    }

}
