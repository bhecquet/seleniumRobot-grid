package com.infotel.seleniumrobot.grid.servlets.client;

import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import kong.unirest.UnirestException;

public interface INodeStatusServletClient {

    SeleniumRobotNode getStatus() throws UnirestException;
    void setStatus(GridStatus newStatus) throws UnirestException;
}
