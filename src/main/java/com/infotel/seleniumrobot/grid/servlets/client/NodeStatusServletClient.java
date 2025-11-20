package com.infotel.seleniumrobot.grid.servlets.client;

import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Client for NodeStatusServlet
 *
 * @author S047432
 */
public class NodeStatusServletClient implements INodeStatusServletClient {

    private static final Logger logger = LogManager.getLogger(NodeStatusServletClient.class.getName());
    private static final String SERVLET_PATH = "/extra/NodeStatusServlet";

    private URI httpHost;

    /**
     * @param host host of node
     * @param port port of node (the one defined at startup, not the servlet port)
     */
    public NodeStatusServletClient(String host, Integer port) throws URISyntaxException {
        this.httpHost = new URI(String.format("http://%s:%d", host, port + 10));
    }

    /**
     * URL of the node
     *
     * @param url
     */
    public NodeStatusServletClient(URL url) throws URISyntaxException {
        this.httpHost = new URI(String.format("http://%s:%d", url.getHost(), url.getPort() + 10));
    }

    /**
     * Returns the json status
     *
     * @throws UnirestException
     */
    public SeleniumRobotNode getStatus() throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get(String.format("%s%s", httpHost.toString(), SERVLET_PATH))
                .queryString("format", "json")
                .asJson();
        if (response.getStatus() != 200) {
            throw new SeleniumGridException(String.format("could not get status from node: %s", response.getStatusText()));
        }
        return SeleniumRobotNode.fromJson(response.getBody().getObject());
    }

    public void setStatus(GridStatus newStatus) throws UnirestException {
        HttpResponse<String> response = Unirest.post(String.format("%s%s", httpHost.toString(), SERVLET_PATH))
                .queryString("status", newStatus.toString())
                .asString();
        if (response.getStatus() != 200) {
            throw new SeleniumGridException(String.format("could not set status %s on node", newStatus.toString()));
        }
    }
}
