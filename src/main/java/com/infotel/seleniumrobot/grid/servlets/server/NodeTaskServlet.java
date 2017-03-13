/**
 * Copyright 2017 www.infotel.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.infotel.seleniumrobot.grid.tasks.NodeRestartTask;
import com.infotel.seleniumrobot.grid.utils.Utils;

/**
 * Servlet for getting all mobile devices information
 * This helps the hub to update capabilities with information on the connected device
 * @author behe
 *
 */
public class NodeTaskServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 216473127866019518L;

	private static final Logger logger = Logger.getLogger(NodeTaskServlet.class);
	
	private NodeRestartTask restartTask = new NodeRestartTask();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		switch (req.getParameter("action")) {
		case "restart":
			logger.info("restarting");
			restartNode();
			break;

		default:
			sendError(resp, String.format("POST Action %s not supported by servlet", req.getParameter("action")));
			break;
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		switch (req.getParameter("action")) {
		case "version":
			sendVersion(resp);
			break;
			
		default:
			sendError(resp, String.format("GET Action %s not supported by servlet", req.getParameter("action")));
			break;
		}
	}
	
	
	
	private void restartNode() {
		try {
			restartTask.execute();
		} catch (Exception e) {
			logger.warn("Could node restart: " + e.getMessage(), e);
		}
	}
	
	private void sendVersion(HttpServletResponse resp) {
		Map<String, String> version = new HashMap<>();
		version.put("version", Utils.getCurrentversion());
		try (
            ServletOutputStream outputStream = resp.getOutputStream()) {
			resp.getOutputStream().print(new JSONObject(version).toString());
        } catch (IOException e) {
        	logger.error("Error sending reply", e);
        }
	}
	
	private void sendError(HttpServletResponse resp, String msg) throws IOException {
		try {
	        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
	        resp.getOutputStream().print(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	
}
