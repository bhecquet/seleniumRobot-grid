package com.infotel.seleniumrobot.grid.servlets.server;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResourceServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String resourceFile;

    /**
     * The file to expose. This must be stored in resources
     * @param resourceFile
     */
    public ResourceServlet(String resourceFile) {
        this.resourceFile = resourceFile;
    }

    /**
     * Allow downloading of files in upload folder
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (this.resourceFile.endsWith(".css")) {
                resp.addHeader("Content-Type", "text/css ");
            }
            if (this.resourceFile.endsWith(".js")) {
                resp.addHeader("Content-Type", "text/javascript");
            }
            IOUtils.copy(getClass().getResourceAsStream(this.resourceFile), resp.getOutputStream());
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error while handling request: " + e.getMessage());
        }
    }

}
