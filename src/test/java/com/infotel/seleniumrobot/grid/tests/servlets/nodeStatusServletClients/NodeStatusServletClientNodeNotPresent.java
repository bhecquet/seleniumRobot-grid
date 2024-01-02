package com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients;

import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import kong.unirest.UnirestException;

public class NodeStatusServletClientNodeNotPresent extends NodeStatusServletClientOk {
    public NodeStatusServletClientNodeNotPresent(String host, Integer port) {
        super(host, port);
    }
    @Override
    public SeleniumRobotNode getStatus() throws UnirestException {
        throw new UnirestException("Node cannot be contacted");
    }
}
