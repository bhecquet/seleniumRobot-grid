package com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients;

import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import kong.unirest.UnirestException;

public class NodeStatusServletClientNodeDisappeared extends NodeStatusServletClientOk {
    public NodeStatusServletClientNodeDisappeared(String host, Integer port) {
        super(host, port);
    }
    @Override
    public SeleniumRobotNode getStatus() throws UnirestException {
        throw new SeleniumGridException("Node not present anymore");
    }
}
