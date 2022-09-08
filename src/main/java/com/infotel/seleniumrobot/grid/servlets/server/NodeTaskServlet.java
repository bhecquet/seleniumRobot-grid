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

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.CleanNodeTask;
import com.infotel.seleniumrobot.grid.tasks.CommandTask;
import com.infotel.seleniumrobot.grid.tasks.DiscoverBrowserAndDriverPidsTask;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.video.DisplayRunningStepTask;
import com.infotel.seleniumrobot.grid.tasks.video.StartVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tasks.video.StopVideoCaptureTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.mobile.AppiumLauncher;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;
import com.seleniumtests.util.video.VideoRecorder;

import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/**
 * Servlet for getting all mobile devices information
 * This helps the hub to update capabilities with information on the connected device
 * @author behe
 *
 */
public class NodeTaskServlet extends GridServlet {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 216473127866019518L;

	private static final Logger logger = LogManager.getLogger(NodeTaskServlet.class);
	private static Map<String, AppiumLauncher> appiumLaunchers = Collections.synchronizedMap(new HashMap<>());

	private Object lock = new Object();

	/**
	 * POST `/extra/NodeTaskServlet?action=<action>` supports several actions
	 * 
	 * - `action=restart`: restart node computer
	 * - `action=kill&process=<process_name>`: kill a process by name without extension
	 * - `action=killPid&pid=<pid>`: kill a process by pid
	 * - `action=leftClic&x=<x_coordinate>&y=<y_coordinate>`: perform a left click at point x,y
	 * - `action=rightClic&x=<x_coordinate>&y=<y_coordinate>`: perform a right click at point x,y
	 * - `action=sendKeys&keycodes=<kc1>,<kc2>` where kcX is a key code. Sends keys to desktop. Used to send non alphanumeric keys
	 * - `action=writeText&text=<text>`: write text to desktop.
	 * - `action=displayRunningStep&stepName=<text>&session=<sessionId>`: write text to desktop.
	 * - `action=uploadFile&name=<file_name>&content=<base64_string>` use browser to upload a file when a upload file window is displayed. The base64 content is copied to a temps file which will then be read by browser.
	 * - `action=setProperty&key=<key>&value=<value>` set java property for the node
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String onlyMainScreenStr = "false";
		switch (req.getParameter("action")) {
			
		// call POST /extra/NodeTaskServlet/kill with process=<task_name>
		case "kill":
			synchronized (lock) {
				killTask(req.getParameter("process"), resp);
			}
			break;
			
		case "killPid":
			synchronized (lock) {
				killPid(Long.parseLong(req.getParameter("pid")), resp);
			}
			break;
			
		// call POST /extra/NodeTaskServlet/leftClic with x=<x-coordinate>,y=<y_coordinate>,onlyMainScreen=<false_or_true>
		case "leftClic":
		case "leftClick":
			onlyMainScreenStr = req.getParameter("onlyMainScreen");
			leftClick(onlyMainScreenStr == null ? false: Boolean.parseBoolean(onlyMainScreenStr), Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")), resp);
			break;
			
		// call POST /extra/NodeTaskServlet/doubleClic with x=<x-coordinate>,y=<y_coordinate>,onlyMainScreen=<false_or_true>
		case "doubleClick":
			onlyMainScreenStr = req.getParameter("onlyMainScreen");
			doubleClick(onlyMainScreenStr == null ? false: Boolean.parseBoolean(onlyMainScreenStr), Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")), resp);
			break;
			
		// call POST /extra/NodeTaskServlet/rightClic with x=<x-coordinate>,y=<y_coordinate>,onlyMainScreen=<false_or_true>
		case "rightClic":
		case "rightClick":
			onlyMainScreenStr = req.getParameter("onlyMainScreen");
			rightClick(onlyMainScreenStr == null ? false: Boolean.parseBoolean(onlyMainScreenStr), Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")), resp);
			break;
			
		// call POST /extra/NodeTaskServlet/sendKeys with keycodes=<kc1>,<kc2> ... where kc is a key code
		case "sendKeys":
			List<Integer> keyCodes = new ArrayList<>();
			for (String kc: req.getParameter("keycodes").split(",")) {
				keyCodes.add(Integer.parseInt(kc));
			}
			sendKeys(keyCodes, resp);
			break;
		
		// call POST /extra/NodeTaskServlet/writeText with text=<text_to_write>
		case "writeText":
			writeText(req.getParameter("text"), resp);
			break;
			
		// call POST /extra/NodeTaskServlet/displayRunningStep with stepName=<step_name>
		case "displayRunningStep":
			displayRunningStep(req.getParameter("stepName"), req.getParameter("session"), resp);
			break;
			
		// call POST /extra/NodeTaskServlet/uploadFile with name=<file_name>,content=<base64_string>
		case "uploadFile":
			String content = "";
			if ("application/octet-stream".equals(req.getContentType())) {
				content = Base64.getEncoder().encodeToString(IOUtils.toByteArray(req.getInputStream()));
			} else {
				content = req.getParameter("content"); // old fashion, for compatibility with core <= 4.21.0;
			}
			

			uploadFile(req.getParameter("name"), content, resp);
			break;
			
		case "setProperty":
			System.setProperty(req.getParameter("key"), req.getParameter("value"));
			break;
			
		case "clean":
			
			try {
				new CleanNodeTask().execute();
			} catch (Exception e) {
				sendError(500, resp, "Error cleaning node");
			}
			break;
			
		// call POST /extra/NodeTaskServlet/command with name=<program_name>,arg0=<arg0>,arg1=<arg1>
		case "command":
			List<String> args = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				String value = req.getParameter(String.format("arg%d", i));
				if (value == null) {
					break;
				} else {
					args.add(value);
				}
			}
			executeCommand(req.getParameter("name"), 
					args, 
					req.getParameter("session"), 
					req.getParameter("timeout") == null ? null: Integer.parseInt(req.getParameter("timeout")), 
					resp);
			break;
			
		default:
			sendError(HttpServletResponse.SC_NOT_FOUND, resp, String.format("POST Action %s not supported by servlet", req.getParameter("action")));
			break;
		}
	}
	
	/**
	 * - `action=version`: returns the version of the node
	 *	- `action=screenshot`: returns a base64 string of the node screen (PNG format)
	 *	- `action=startVideoCapture&session=<test_session_id>`: start video capture on the node. SessionId is used to store the video file
	 *	- `action=stopVideoCapture&session=<test_session_id>`: stop video capture previously created (use the provided sessionId)
	 *	- `action=startAppium&session=<test_session_id>`: start appium server 
	 *	- `action=stopAppium&session=<test_session_id>`: stop the appium server previously started with corresponding sessionId
	 *	- `action=driverPids&browserName=<browser>&browserVersion=<version>&existingPids=<some_pids>`: Returns list of PIDS for this driver exclusively. This allows the hub to know which browser has been recently started. If existingPids is not empty, these pids won't be returned by the command. Browser name and version refers to installed browsers, declared in grid node
	 *	- `action=browserAndDriverPids&browserName=<browser>&browserVersion=<version>&parentPids=<some_pid>`: Returns list of PIDs for this driver and for all subprocess created (driver, browser and other processes). This allows to kill any process created by a driver. parentPids are the processs for which we should search child processes.
	 *	- `action=keepAlive`: move mouse from 1 pixel so that windows session does not lock
	 *  - `action=processList&name=myprocessName`: returns the list of PIDs whose process name is the requested one
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		switch (req.getParameter("action")) {
		case "version":
			sendVersion(resp);
			break;
			
		case "screenshot":
			takeScreenshot(resp);
			break;	
			
		case "startVideoCapture":
			startVideoCapture(req.getParameter("session"), resp);
			break;
			
		case "stopVideoCapture":
			stopVideoCapture(req.getParameter("session"), resp);
			break;
			
		case "startAppium":
			startAppium(req.getParameter("session"), resp);
			break;
			
		case "stopAppium":
			stopAppium(req.getParameter("session"), resp);
			break;

		case "mouseCoordinates":
			mouseCoordinates(resp);
			break;
			
		case "driverPids":
			String existingPidsStr = req.getParameter("existingPids");
			List<Long> existingPids = existingPidsStr.isEmpty() ? new ArrayList<>(): Arrays.asList(existingPidsStr.split(","))
					.stream()
					.map(Long::parseLong)
					.collect(Collectors.toList());
			getBrowserPids(req.getParameter("browserName"), req.getParameter("browserVersion"), existingPids, resp);
			break;
			
		case "browserAndDriverPids":
			String parentPidsStr = req.getParameter("parentPids");
			List<Long> parentPids = parentPidsStr.isEmpty() ? new ArrayList<>(): Arrays.asList(parentPidsStr.split(","))
					.stream()
					.map(Long::parseLong)
					.collect(Collectors.toList());
			getAllBrowserSubprocessPids(req.getParameter("browserName"), req.getParameter("browserVersion"), parentPids, resp);
			break;
			
		case "processList":
			String processName = req.getParameter("name");
			getProcessList(processName, resp);
			break;
		
		default:
			sendError(HttpServletResponse.SC_NOT_FOUND, resp, String.format("GET Action %s not supported by servlet", req.getParameter("action")));
			break;
		}
	}
	
	/**
	 * Send a command to the driver to keep it alive
	 * @param session
	 * @throws UnirestException
	 */
	private void keepDriverAlive(String session) throws UnirestException {
		Unirest.get(String.format("%s/wd/hub/session/%s/url", LaunchConfig.getCurrentNodeConfig().getNodeOptions().getPublicGridUri().toString(), session))
				.asString();
	}
	
	private void executeCommand(String commandName, List<String> args, String sessionKey, Integer timeout, HttpServletResponse resp) throws IOException {
		
		ExecutorService executorService = null;
		if (sessionKey != null) {
			logger.info("disable timeout during command execution");

			executorService = Executors.newSingleThreadExecutor();
			executorService.submit(() -> {
				while (true) {
					keepDriverAlive(sessionKey);
					try {
						System.out.println("waiting");
						Thread.sleep(15000);
					} catch (InterruptedException e) {
						break;
					}
				}
		      });
		}
		
		logger.info("executing command " + commandName);
		try {
			CommandTask commandTask = CommandTask.getInstance();
			commandTask.setCommand(commandName, args, timeout);
			commandTask.execute();
			sendOk(resp, commandTask.getResult());
		} catch (Exception e) {
			logger.warn("Could not exeecute command: " + e.getMessage(), e);
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		} finally {
			if (sessionKey != null) {
				logger.info("enable timeout after command execution");
				executorService.shutdownNow();
				logger.info("timeout enabled");
			}
		}
	}
	
	private void killTask(String taskName, HttpServletResponse resp) throws IOException {
		logger.info("killing process " + taskName);
		try {
			assert taskName != null;
			new KillTask().withName(taskName)
				.execute();
			sendOk(resp, "process killed");
		} catch (Exception e) {
			logger.warn("Could not kill process: " + e.getMessage(), e);
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}	
	}
	
	private void killPid(Long pid, HttpServletResponse resp) throws IOException {
		logger.info("killing process " + pid);
		try {
			assert pid != null;
			new KillTask().withPid(pid)
				.execute();
			sendOk(resp, "process killed");
		} catch (Exception e) {
			logger.warn("Could not kill process: " + e.getMessage(), e);
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}	
	}
	
	/**
	 * Left clic on desktop
	 * @param x
	 * @param y
	 * @throws IOException 
	 */
	private void leftClick(boolean onlyMainScreen, int x, int y, HttpServletResponse resp) throws IOException {
		try {
			logger.info(String.format("left clic at %d,%d", x, y));
			CustomEventFiringWebDriver.leftClicOnDesktopAt(onlyMainScreen, x, y, DriverMode.LOCAL, null);
			sendOk(resp, "left clic ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	private void mouseCoordinates(HttpServletResponse resp) throws IOException {
		try {
			logger.info("mouse coordinates");
			Point coords = CustomEventFiringWebDriver.getMouseCoordinates(DriverMode.LOCAL, null);
			sendOk(resp, String.format("%d,%d", coords.x, coords.y));
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * double clic on desktop
	 * @param x
	 * @param y
	 * @throws IOException 
	 */
	private void doubleClick(boolean onlyMainScreen, int x, int y, HttpServletResponse resp) throws IOException {
		try {
			logger.info(String.format("left clic at %d,%d", x, y));
			CustomEventFiringWebDriver.doubleClickOnDesktopAt(onlyMainScreen, x, y, DriverMode.LOCAL, null);
			sendOk(resp, "double clic ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * right clic on desktop
	 * @param x
	 * @param y
	 * @throws IOException 
	 */
	private void rightClick(boolean onlyMainScreen, int x, int y, HttpServletResponse resp) throws IOException {
		try {
			logger.info(String.format("right clic at %d,%d", x, y));
			CustomEventFiringWebDriver.rightClicOnDesktopAt(onlyMainScreen, x, y, DriverMode.LOCAL, null);
			sendOk(resp, "right clic ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * screenshot of the desktop
	 * @throws IOException 
	 */
	private void takeScreenshot(HttpServletResponse resp) throws IOException {
		logger.info("taking screenshot");
		try  {
			ServletOutputStream outputStream = resp.getOutputStream();
			outputStream.print(CustomEventFiringWebDriver.captureDesktopToBase64String(DriverMode.LOCAL, null));
			outputStream.flush();
        } catch (IOException e) {
        	logger.error("Error sending reply", e);
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
        }
	}
	
	/**
	 * Send key events
	 * @param keys
	 * @throws IOException 
	 */
	private void sendKeys(List<Integer> keys, HttpServletResponse resp) throws IOException {
		try {
			logger.info("sending keys: " + keys);
			CustomEventFiringWebDriver.sendKeysToDesktop(keys, DriverMode.LOCAL, null);
			sendOk(resp, "send keys");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * write text using keyboard
	 * @param text
	 * @throws IOException 
	 */
	private void writeText(String text, HttpServletResponse resp) throws IOException {
		try {
			logger.info("writing text: " + text);
			CustomEventFiringWebDriver.writeToDesktop(text, DriverMode.LOCAL, null);
			sendOk(resp, "write text ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * write text using keyboard
	 * @param text
	 * @throws IOException 
	 */
	private void displayRunningStep(String stepName, String sessionId, HttpServletResponse resp) throws IOException {
		try {
			logger.info("display step: " + stepName);
			new DisplayRunningStepTask(stepName, sessionId).execute();
			sendOk(resp, "display step ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * upload file to browser
	 * 
	 * @param fileName		name of the file to upload 
	 * @param fileContent	content as a base64 string
	 * @throws IOException 
	 */
	private void uploadFile(String fileName, String fileContent, HttpServletResponse resp) throws IOException {
		try {
			logger.info("uploading file: " + fileName);
			CustomEventFiringWebDriver.uploadFile(fileName, fileContent, DriverMode.LOCAL, null);
			sendOk(resp, "upload file ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	private void sendVersion(HttpServletResponse resp) {
		Map<String, String> version = new HashMap<>();
		version.put("version", Utils.getCurrentversion());
		try (
            ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(new JSONObject(version).toString());
        } catch (IOException e) {
        	logger.error("Error sending reply", e);
        }
	}
	
	private void startVideoCapture(String sessionId, HttpServletResponse resp) throws IOException {
		try {
			logger.info("ssessionIdo capture for session: " + sessionId);
			
			new StartVideoCaptureTask(sessionId).execute();
			
			sendOk(resp, "start video ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * Stop capture, send file and delete it afterwards
	 * @param sessionId
	 * @param resp
	 * @throws IOException
	 */
	private void stopVideoCapture(String sessionId, HttpServletResponse resp) throws IOException {
		logger.info("stop video capture for session: " + sessionId);
		try {
			File videoFile = new StopVideoCaptureTask(sessionId)
				.execute()
				.getVideoFile();
			
			if (videoFile != null) {
	            ServletOutputStream outputStream = resp.getOutputStream();
				outputStream.write(FileUtils.readFileToByteArray(videoFile));
				outputStream.flush();
			}
			logger.info("video capture stopped");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	private void startAppium(String sessionId, HttpServletResponse resp) throws IOException {
		logger.info("starting appium");

    	try {
    		String logDir = Paths.get(Utils.getRootdir(), "logs", "appium").toString();

			// start appium before creating instance
			AppiumLauncher appiumLauncher = new LocalAppiumLauncher(logDir);
	    	appiumLauncher.startAppium();
	    	appiumLaunchers.put(sessionId, appiumLauncher);
	    	
	    	logger.info("appium is running on " + ((LocalAppiumLauncher)appiumLauncher).getAppiumServerUrl());
    	
    		ServletOutputStream outputStream = resp.getOutputStream();
			outputStream.print(((LocalAppiumLauncher)appiumLauncher).getAppiumServerUrl());
			outputStream.flush();
        } catch (Exception e) {
        	logger.error("Error sending appium URL", e);
        	sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
        }
	}
	
	private void stopAppium(String sessionId, HttpServletResponse resp) throws IOException {
		try {
			AppiumLauncher appiumLauncher = appiumLaunchers.remove(sessionId);
			if (appiumLauncher != null) {
				appiumLauncher.stopAppium();
			}
			sendOk(resp, "stop appium ok");
		} catch (Exception e) {
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	/**
	 * Returns list of PID for the given browser
	 * @param browserType
	 * @param browserVersion
	 * @param resp
	 * @throws IOException 
	 */
	private void getBrowserPids(String browserName, String browserVersion, List<Long> existingPids, HttpServletResponse resp) throws IOException {
		logger.info("get driver pids for browser " + browserName);
		
		
		try  {
			ServletOutputStream outputStream = resp.getOutputStream();

			// get pid pre-existing the creation of this driver. This helps filtering drivers launched by other tests or users
			List<Long> pidsToReturn = new DiscoverBrowserAndDriverPidsTask(browserName, browserVersion)
					.withExistingPids(existingPids)
					.execute()
					.getProcessPids();
			
			outputStream.print(StringUtils.join(pidsToReturn, ","));
			outputStream.flush();
        } catch (Exception e) {
        	logger.error("Error sending browser pids", e);
        	sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
        }
	}
	
	/**
	 * Returns list of PIDs corresponding to driver and browser (+ processes that could have been created by browser)
	 * @param browserType
	 * @param browserVersion
	 * @param resp
	 * @throws IOException 
	 */
	private void getAllBrowserSubprocessPids(String browserName, String browserVersion, List<Long> parentPids, HttpServletResponse resp) throws IOException {
		logger.info("get browser/driver pids for browser " + browserName);

		try {
			ServletOutputStream outputStream = resp.getOutputStream();
			List<Long> subProcessPids = new DiscoverBrowserAndDriverPidsTask(browserName, browserVersion)
					.withParentsPids(parentPids)
					.execute()
					.getProcessPids();
			
			outputStream.print(StringUtils.join(subProcessPids, ","));
			outputStream.flush();
		} catch (Exception e) {
			logger.error("Error sending browser/driver pids", e);
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
		}
	}
	
	
	
	/**
	 * Send to requester, the list of PIDs whose name is the requested process name
	 * @param processName
	 * @throws IOException 
	 */
	private void getProcessList(String processName, HttpServletResponse resp) throws IOException {
		
		try  {
			
			List<ProcessInfo> processList = OSUtilityFactory.getInstance().getRunningProcesses(processName);
			List<String> pidsToReturn = processList.stream()
					.map(p -> p.getPid())
					.collect(Collectors.toList());
			
			ServletOutputStream outputStream = resp.getOutputStream();
			outputStream.print(StringUtils.join(pidsToReturn, ","));
			outputStream.flush();
        } catch (Exception e) {
        	logger.error("Error sending browser pids", e);
        	sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp, e.getMessage());
        }
	}

	/***** for tests */
	public static Map<String, AppiumLauncher> getAppiumLaunchers() {
		return appiumLaunchers;
	}
	
	public static void resetAppiumLaunchers() {
		appiumLaunchers = Collections.synchronizedMap(new HashMap<>());
	}

}
