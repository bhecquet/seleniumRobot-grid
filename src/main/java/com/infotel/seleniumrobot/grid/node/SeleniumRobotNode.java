package com.infotel.seleniumrobot.grid.node;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.SeleniumGridException;
import com.infotel.seleniumrobot.grid.servlets.server.WebServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.grid.config.Config;
import org.openqa.selenium.grid.data.*;
import org.openqa.selenium.grid.log.LoggingOptions;
import org.openqa.selenium.grid.node.HealthCheck;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.config.NodeOptions;
import org.openqa.selenium.grid.node.local.LocalNodeFactory;
import org.openqa.selenium.grid.security.Secret;
import org.openqa.selenium.grid.security.SecretOptions;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.internal.Either;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.Filter;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Routable;
import org.openqa.selenium.remote.tracing.Tracer;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

public class SeleniumRobotNode extends Node {

    private static Logger logger = LogManager.getLogger(SeleniumRobotNode.class);

    private Node node;

    protected SeleniumRobotNode(Tracer tracer, NodeId nodeId, URI uri, Secret registrationSecret, Duration sessionTimeout) {
        super(tracer, nodeId, uri, registrationSecret, sessionTimeout);
    }

    /**
     * Copied from LocalNodeFactory
     *
     * @param config
     * @return
     */
    public static Node create(Config config) {
        LoggingOptions loggingOptions = new LoggingOptions(config);
        BaseServerOptions serverOptions = new BaseServerOptions(config);
        URI uri = serverOptions.getExternalUri();
        SecretOptions secretOptions = new SecretOptions(config);

        NodeOptions nodeOptions = new NodeOptions(config);

        // store configuration
        LaunchConfig.getCurrentNodeConfig().setServerOptions(new BaseServerOptions(config));
        LaunchConfig.getCurrentNodeConfig().setNodeOptions(new NodeOptions(config));

        // add servlets
        try {
            new WebServer().startNodeServletServer(new BaseServerOptions(config).getPort() + 10);
        } catch (Exception e) {
            throw new SeleniumGridException("Error starting servlet server");
        }

        logger.info("Adding servlets");

        Node node = LocalNodeFactory.create(config);

        SeleniumRobotNode wrapper = new SeleniumRobotNode(loggingOptions.getTracer(),
                node.getId(),
                uri,
                secretOptions.getRegistrationSecret(),
                nodeOptions.getSessionTimeout());
        wrapper.node = node;
        return wrapper;
    }


    @Override
    public Either<WebDriverException, CreateSessionResponse> newSession(
            CreateSessionRequest sessionRequest) {
        return perform(() -> node.newSession(sessionRequest), "newSession");
    }

    @Override
    public HttpResponse executeWebDriverCommand(HttpRequest req) {
        return perform(() -> node.executeWebDriverCommand(req), "executeWebDriverCommand");
    }

    @Override
    public Session getSession(SessionId id) throws NoSuchSessionException {
        return perform(() -> node.getSession(id), "getSession");
    }

    @Override
    public HttpResponse uploadFile(HttpRequest req, SessionId id) {
        return perform(() -> node.uploadFile(req, id), "uploadFile");
    }

    @Override
    public HttpResponse downloadFile(HttpRequest req, SessionId id) {
        return perform(() -> node.downloadFile(req, id), "downloadFile");
    }

    @Override
    public TemporaryFilesystem getDownloadsFilesystem(SessionId id) {
        return perform(() -> {
            try {
                return node.getDownloadsFilesystem(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "downloadsFilesystem");
    }

    @Override
    public TemporaryFilesystem getUploadsFilesystem(SessionId id) throws IOException {
        return perform(() -> {
            try {
                return node.getUploadsFilesystem(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "uploadsFilesystem");

    }

    @Override
    public void stop(SessionId id) throws NoSuchSessionException {
        perform(() -> node.stop(id), "stop");
    }

    @Override
    public boolean isSessionOwner(SessionId id) {
        return perform(() -> node.isSessionOwner(id), "isSessionOwner");
    }

    @Override
    public boolean tryAcquireConnection(SessionId id) {
        return perform(() -> node.tryAcquireConnection(id), "tryAcquireConnection");
    }

    @Override
    public void releaseConnection(SessionId id) {

    }

    @Override
    public boolean isSupporting(Capabilities capabilities) {
        return perform(() -> node.isSupporting(capabilities), "isSupporting");
    }

    @Override
    public NodeStatus getStatus() {
        keepAlive();
        return perform(() -> node.getStatus(), "getStatus");
    }

    @Override
    public HealthCheck getHealthCheck() {
        return perform(() -> node.getHealthCheck(), "getHealthCheck");
    }

    @Override
    public void drain() {
        perform(() -> node.drain(), "drain");
    }

    @Override
    public boolean isReady() {
        return perform(() -> node.isReady(), "isReady");
    }

    private void perform(Runnable function, String operation) {
        function.run();
    }

    private <T> T perform(Supplier<T> function, String operation) {
        return function.get();
    }

    public void keepAlive() {

        // do not clear drivers and browser when devMode is true
        if (!LaunchConfig.getCurrentLaunchConfig().getDevMode() && LaunchConfig.getCurrentLaunchConfig().getKeepSessionOpened()) {
            Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
            if (mouseLocation != null) {
                double choice = Math.random();
                try {
                    if (choice > 0.5) {
                        new Robot().mouseMove(mouseLocation.x - 1, mouseLocation.y);
                    } else {
                        new Robot().mouseMove(mouseLocation.x + 1, mouseLocation.y);
                    }
                } catch (AWTException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public Routable with(Filter filter) {
        return perform(() -> node.with(filter), "with");
    }
}
