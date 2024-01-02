package com.infotel.seleniumrobot.grid.tests.servlets.nodeStatusServletClients;

import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import kong.unirest.UnirestException;

public class NodeStatusServletClientSetStatusKo extends NodeStatusServletClientOk {
    public NodeStatusServletClientSetStatusKo(String host, Integer port) {
        super(host, port);
    }
    @Override
    public void setStatus(GridStatus newStatus) throws UnirestException {
        throw new SeleniumGridException("cannot set status");
    }
}
