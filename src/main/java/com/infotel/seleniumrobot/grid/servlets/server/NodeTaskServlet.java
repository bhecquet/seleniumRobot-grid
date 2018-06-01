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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.NodeRestartTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;

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
	private KillTask killTask = new KillTask();
	
	private Object lock = new Object();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		switch (req.getParameter("action")) {
		case "restart":
			restartNode();
			break;
			
		// call POST /extra/NodeTaskServlet/kill with process=<task_name>
		case "kill":
			synchronized (lock) {
				killTask(req.getParameter("process"));
			}
			break;
			
		// call POST /extra/NodeTaskServlet/leftClic with x=<x-coordinate>,y=<y_coordinate>
		case "leftClic":
			leftClic(Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")));
			break;
			
		// call POST /extra/NodeTaskServlet/rightClic with x=<x-coordinate>,y=<y_coordinate>
		case "rightClic":
			rightClic(Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")));
			break;
			
		// call POST /extra/NodeTaskServlet/sendKeys with keycodes=<kc1>,<kc2> ... where kc is a key code
		case "sendKeys":
			List<Integer> keyCodes = new ArrayList<>();
			for (String kc: req.getParameter("keycodes").split(",")) {
				keyCodes.add(Integer.parseInt(kc));
			}
			sendKeys(keyCodes);
			break;
		
		// call POST /extra/NodeTaskServlet/writeText with text=<text_to_write>
		case "writeText":
			writeText(req.getParameter("text"));
			break;
			
		// call POST /extra/NodeTaskServlet/uploadFile with name=<file_name>,content=<base64_string>
		case "uploadFile":
			uploadFile(req.getParameter("name"), req.getParameter("content"));
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
		case "screenshot":
			takeScreenshot(resp);
			break;	
		
		default:
			sendError(resp, String.format("GET Action %s not supported by servlet", req.getParameter("action")));
			break;
		}
	}
	
	private void killTask(String taskName) {
		logger.info("killing process " + taskName);
		try {
			assert taskName != null;
			killTask.setTaskName(taskName);
			killTask.execute();
		} catch (Exception e) {
			logger.warn("Could not kill process: " + e.getMessage(), e);
		}	
	}
	
	private void restartNode() {
		logger.info("restarting");
		try {
			restartTask.execute();
		} catch (Exception e) {
			logger.warn("Could not restart node: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Left clic on desktop
	 * @param x
	 * @param y
	 */
	private void leftClic(int x, int y) {
		logger.info(String.format("left clic at %d,%d", x, y));
		CustomEventFiringWebDriver.leftClicOnDesktopAt(x, y, DriverMode.LOCAL, null);
	}
	
	/**
	 * right clic on desktop
	 * @param x
	 * @param y
	 */
	private void rightClic(int x, int y) {
		logger.info(String.format("right clic at %d,%d", x, y));
		CustomEventFiringWebDriver.rightClicOnDesktopAt(x, y, DriverMode.LOCAL, null); 
	}
	
	/**
	 * screenshot of the desktop
	 */
	private void takeScreenshot(HttpServletResponse resp) {
		logger.info("taking screenshot");
		try (
            ServletOutputStream outputStream = resp.getOutputStream()) {
			resp.getOutputStream().print(CustomEventFiringWebDriver.captureDesktopToBase64String(DriverMode.LOCAL, null));
        } catch (IOException e) {
        	logger.error("Error sending reply", e);
        }
	}
	
	/**
	 * Send key events
	 * @param keys
	 */
	private void sendKeys(List<Integer> keys) {
		logger.info("sending keys: " + keys);
		CustomEventFiringWebDriver.sendKeysToDesktop(keys, DriverMode.LOCAL, null);
	}
	
	/**
	 * write text using keyboard
	 * @param text
	 */
	private void writeText(String text) {
		logger.info("writing text: " + text);
		CustomEventFiringWebDriver.writeToDesktop(text, DriverMode.LOCAL, null);
	}
	
	/**
	 * upload file to browser
	 * 
	 * @param fileName		name of the file to upload 
	 * @param fileContent	content as a base64 string
	 * @throws IOException 
	 */
	private void uploadFile(String fileName, String fileContent) throws IOException {
		logger.info("uploading file: " + fileName);
		CustomEventFiringWebDriver.uploadFile(fileName, fileContent, DriverMode.LOCAL, null);
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
