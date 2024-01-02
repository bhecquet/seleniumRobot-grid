package com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients;

import com.infotel.seleniumrobot.grid.servlets.client.INodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class NodeStatusServletClientOk implements INodeStatusServletClient {


    public static Map<INodeStatusServletClient, GridStatus> statusMap = new LinkedHashMap<>();

    public NodeStatusServletClientOk(String host, Integer port) {}

    SeleniumRobotNode seleniumRobotNode = SeleniumRobotNode.fromJson(new JSONObject("{" +
                                                                          "  \"memory\": {" +
                                                                          "    \"totalMemory\": 17054," +
                                                                          "    \"class\": \"com.infotel.seleniumrobot.grid.utils.MemoryInfo\"," +
                                                                          "    \"freeMemory\": 5035" +
                                                                          "  }," +
                                                                          "  \"maxSessions\": 1," +
                                                                          "  \"port\": 4444," +
                                                                          "  \"ip\": \"SN782980\"," +
                                                                          "  \"cpu\": 0.0," +
                                                                          "  \"version\": \"5.1.0\"," +
                                                                          "  \"hub\": \"localhost\"," +
                                                                          "  \"nodeTags\": \"foo,bar\"," +
                                                                          "  \"driverVersion\": \"5.0.0\"," +
                                                                          "  \"status\": \"ACTIVE\"" +
                                                                          "}"));

    @Override
    public SeleniumRobotNode getStatus() throws UnirestException {
        return seleniumRobotNode;
    }

    @Override
    public void setStatus(GridStatus newStatus) throws UnirestException {
        statusMap.put(this, newStatus);
    }
}
