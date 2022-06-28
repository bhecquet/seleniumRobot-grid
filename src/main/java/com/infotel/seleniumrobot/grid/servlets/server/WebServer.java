package com.infotel.seleniumrobot.grid.servlets.server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Expose one web resource stored in src/test/resources on localhost
 * @author behe
 *
 */
public class WebServer {

	private Server server;

    public void startNodeServletServer(int port) throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});
        
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        
        servletHandler.addServletWithMapping(NodeStatusServlet.class, "/extra/NodeStatusServlet");
        servletHandler.addServletWithMapping(NodeTaskServlet.class, "/extra/NodeTaskServlet");
        servletHandler.addServletWithMapping(FileServlet.class, "/extra/FileServlet");
        server.start();
    }

    private Map<String, String> getRouterResourceMapping() {

        Map<String, String> mapping = new HashMap<>();
        for (String path: new String[] { "css/bootstrap.min.css",
                                        "css/bootstrap.min.js",
                                        "css/hubCss.css",
                                        "css/iframecss.css",
                                        "css/report.css",
                                        "img/background.jpg",
                                        "img/config.png",
                                        "img/infotel.png",
                                        "img/node.png",
                                        "img/seleniumlogo_low.png",
                                        "img/sessions.png",
                                        "img/up.png",
                                        "js/status.js"
        }) {
            mapping.put("/templates/" + path, "/grid/resources/templates/" + path);
        }

        return mapping;
    }
    
    public void startRouterServletServer(int port) throws Exception {
    	server = new Server();
    	ServerConnector connector = new ServerConnector(server);
    	connector.setPort(port);
    	server.setConnectors(new Connector[] {connector});
    	
    	ServletHandler servletHandler = new ServletHandler();
    	server.setHandler(servletHandler);

        for (Map.Entry<String, String> entry: getRouterResourceMapping().entrySet()) {
            servletHandler.addServletWithMapping(new ServletHolder(new ResourceServlet(entry.getKey())), entry.getValue());
        }
    	
    	servletHandler.addServletWithMapping(GuiServlet.class, "/grid/admin/GuiServlet");
    	servletHandler.addServletWithMapping(FileServlet.class, "/grid/admin/FileServlet");
    	server.start();
    }	
	
}
