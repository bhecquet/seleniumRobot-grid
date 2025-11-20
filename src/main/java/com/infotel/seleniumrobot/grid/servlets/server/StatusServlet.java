package com.infotel.seleniumrobot.grid.servlets.server;

import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.client.GridStatusClient;
import com.infotel.seleniumrobot.grid.servlets.client.INodeStatusServletClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeStatusServletClientFactory;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNode;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumRobotNode;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kong.unirest.core.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.json.Json;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StatusServlet extends GridServlet {

    private final Json json = new Json();
    private Configuration jsonPathConf;
    public static final String STATUS = "status";
    private GridStatusClient gridStatusClient;
    private NodeStatusServletClientFactory nodeStatusServletClientFactory;
    private static final Logger logger = LogManager.getLogger(StatusServlet.class.getName());

    public StatusServlet() throws MalformedURLException {
        this(new GridStatusClient(new URL(String.format("http://%s:%d", LaunchConfig.getCurrentLaunchConfig().getRouterHost(), LaunchConfig.getCurrentLaunchConfig().getRouterPort()))),
                new NodeStatusServletClientFactory());
    }

    public StatusServlet(GridStatusClient gridStatusClient, NodeStatusServletClientFactory nodeStatusServletClientFactory) {
        super();

        this.gridStatusClient = gridStatusClient;
        this.nodeStatusServletClientFactory = nodeStatusServletClientFactory;
        jsonPathConf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        sendStatus(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        setStatus(request, response);
    }

    /**
     * Update hub status and all node statuses
     *
     * @param request
     * @param response
     */
    private void setStatus(HttpServletRequest request, HttpServletResponse response) {

        String msg = "OK";
        int statusCode = 200;

        try {
            GridStatus status = GridStatus.fromString(request.getParameter("status"));

            if (!(GridStatus.ACTIVE.equals(status) || GridStatus.INACTIVE.equals(status))) {
                new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Status must be 'active' or 'inactive'").send(response);
                return;
            }

            for (SeleniumNode node : gridStatusClient.getNodes()) {
                URL nodeUrl = new URL(node.getExternalUri());
                nodeStatusServletClientFactory.createNodeStatusServletClient(nodeUrl.getHost(), nodeUrl.getPort()).setStatus(status);
            }
            new ServletResponse(HttpServletResponse.SC_OK, msg, MediaType.JSON_UTF_8).send(response);
            return;

        } catch (IllegalArgumentException e) {
            msg = "you must provide a 'status' parameter (either 'active' or 'inactive')";
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        } catch (UnirestException | SeleniumGridException | MalformedURLException e) {
            msg = String.format("Error while forwarding status to node: %s", e.getMessage());
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            new ServletResponse(statusCode, msg).send(response);
        } catch (Throwable e) {
            throw new SeleniumGridException(e.getMessage());
        }
    }

    /**
     * returns the hub and node status
     *
     * @param request
     * @param response
     */
    private void sendStatus(HttpServletRequest request, HttpServletResponse response) {

        try {
            Object status = buildStatus(request);
            String reply = json.toJson(status);

            // do we return the whole status or just a part of it ?
            String jsonPath = request.getParameter("jsonpath");
            if (jsonPath != null) {
                status = JsonPath.using(jsonPathConf).parse(reply).read(jsonPath);
                reply = status instanceof String ? status.toString() : json.toJson(status);
            }

            new ServletResponse(HttpServletResponse.SC_OK, reply, MediaType.JSON_UTF_8).send(response);
        } catch (Throwable e) {
            logger.error("Error sending status", e);
            throw new SeleniumGridException(e.getMessage());
        }
    }

    private Map<String, Object> buildStatus(HttpServletRequest request) {


        Map<String, Object> status = new TreeMap<>();
        status.put("success", true);
        status.put("hub", buildHubStatus());

        for (SeleniumNode node : gridStatusClient.getNodes()) {

            try {
                URL nodeUrl = new URL(node.getExternalUri());

                INodeStatusServletClient nodeStatusServletClient = nodeStatusServletClientFactory.createNodeStatusServletClient(nodeUrl);
                SeleniumRobotNode nodeStatus = nodeStatusServletClient.getStatus();

                Map<String, Object> nodeInfos = new HashMap<>();
                nodeInfos.put("busy", node.isBusy());
                nodeInfos.put("version", nodeStatus.getVersion());
                nodeInfos.put("driverVersion", nodeStatus.getDriverVersion());
                nodeInfos.put("testSlots", nodeStatus.getMaxSessions());
                nodeInfos.put("usedTestSlots", node.getSessionList().size());
                nodeInfos.put("lastSessionStart", node.getLastStarted().format(DateTimeFormatter.ISO_DATE_TIME));
                nodeInfos.put("status", nodeStatus.getNodeStatus());


                status.put(node.getExternalUri(), nodeInfos);
            } catch (Exception e) {
                continue;
            }
        }


        return status;
    }

    /**
     * Build hub status
     *
     * @return
     */
    private Map<String, String> buildHubStatus() {

        Map<String, String> hubInfos = new HashMap<>();

        hubInfos.put("version", Utils.getCurrentversion());

        return hubInfos;
    }

}