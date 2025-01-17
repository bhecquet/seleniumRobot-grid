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

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.common.net.MediaType;
import kong.unirest.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.CleanNodeTask;
import com.infotel.seleniumrobot.grid.tasks.CommandTask;
import com.infotel.seleniumrobot.grid.tasks.DiscoverBrowserAndDriverPidsTask;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.video.DisplayRunningStepTask;
import com.infotel.seleniumrobot.grid.tasks.video.StartVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tasks.video.StopVideoCaptureTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;

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
		ServletResponse response = executePostAction(req.getParameterMap(), req.getContentType(), req.getInputStream());
		response.send(resp);
	}

	public ServletResponse executePostAction(Map<String, String[]> parameters, String contentType, InputStream inputStream) {
		String onlyMainScreenStr = "false";
		switch (parameters.get("action")[0]) {
			
		// call POST /extra/NodeTaskServlet/action=kill with process=<task_name>
		case "kill":
			synchronized (lock) {
				return killTask(parameters.get("process")[0]);
			}
			
		case "killPid":
			synchronized (lock) {
				return killPid(Long.parseLong(parameters.get("pid")[0]));
			}
			
		// call POST /extra/NodeTaskServlet/leftClic with x=<x-coordinate>,y=<y_coordinate>,onlyMainScreen=<false_or_true>
		case "leftClic":
		case "leftClick":
			onlyMainScreenStr = parameters.getOrDefault("onlyMainScreen", new String[]{null})[0];
			return leftClick(onlyMainScreenStr == null ? false: Boolean.parseBoolean(onlyMainScreenStr), Integer.parseInt(parameters.get("x")[0]), Integer.parseInt(parameters.get("y")[0]));
			
		// call POST /extra/NodeTaskServlet/doubleClic with x=<x-coordinate>,y=<y_coordinate>,onlyMainScreen=<false_or_true>
		case "doubleClick":
			onlyMainScreenStr = parameters.getOrDefault("onlyMainScreen", new String[]{null})[0];
			return doubleClick(onlyMainScreenStr == null ? false: Boolean.parseBoolean(onlyMainScreenStr), Integer.parseInt(parameters.get("x")[0]), Integer.parseInt(parameters.get("y")[0]));
			
		// call POST /extra/NodeTaskServlet/rightClic with x=<x-coordinate>,y=<y_coordinate>,onlyMainScreen=<false_or_true>
		case "rightClic":
		case "rightClick":
			onlyMainScreenStr = parameters.getOrDefault("onlyMainScreen", new String[]{null})[0];
			return rightClick(onlyMainScreenStr == null ? false: Boolean.parseBoolean(onlyMainScreenStr), Integer.parseInt(parameters.get("x")[0]), Integer.parseInt(parameters.get("y")[0]));
			
		// call POST /extra/NodeTaskServlet/sendKeys with keycodes=<kc1>,<kc2> ... where kc is a key code
		case "sendKeys":
			List<Integer> keyCodes = new ArrayList<>();
			for (String kc: parameters.get("keycodes")[0].split(",")) {
				keyCodes.add(Integer.parseInt(kc));
			}
			return sendKeys(keyCodes);
		
		// call POST /extra/NodeTaskServlet/writeText with text=<text_to_write>
		case "writeText":
			return writeText(parameters.get("text")[0]);
			
		// call POST /extra/NodeTaskServlet/displayRunningStep with stepName=<step_name>
		case "displayRunningStep":
			return displayRunningStep(parameters.get("stepName")[0], parameters.get("session")[0]);
			
		// call POST /extra/NodeTaskServlet/uploadFile with name=<file_name>,content=<base64_string>
		case "uploadFile":
			String content = "";
			try {
				content = Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
			} catch (IOException e) {
				return new ServletResponse(HttpServletResponse.SC_BAD_REQUEST, "Cannot decode input stream");
			}

			return uploadFile(parameters.get("name")[0], content);
			
		case "setProperty":
			System.setProperty(parameters.get("key")[0], parameters.get("value")[0]);
			return new ServletResponse(HttpServletResponse.SC_OK, "Property set");
			
		case "clean":
			
			try {
				new CleanNodeTask().execute();
				return new ServletResponse(HttpServletResponse.SC_OK, "clean done");
			} catch (Exception e) {
				return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error cleaning node");
			}
			
		// call POST /extra/NodeTaskServlet/command with name=<program_name>,arg0=<arg0>,arg1=<arg1>
		case "command":
			List<String> args = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				String value = parameters.getOrDefault(String.format("arg%d", i), new String[]{null})[0];
				if (value == null) {
					break;
				} else {
					args.add(value);
				}
			}
			return executeCommand(parameters.get("name")[0],
					args, 
					parameters.getOrDefault("session", new String[]{null})[0],
					parameters.getOrDefault("timeout", new String[]{null})[0] == null ? null: Integer.parseInt(parameters.get("timeout")[0]));
			
		default:
			return new ServletResponse(HttpServletResponse.SC_NOT_FOUND, String.format("POST Action %s not supported by servlet", parameters.get("action")[0]));
		}
	}
	
	/**
	 * - `action=version`: returns the version of the node
	 *	- `action=screenshot`: returns a base64 string of the node screen (PNG format)
	 *	- `action=startVideoCapture&session=<test_session_id>`: start video capture on the node. SessionId is used to store the video file
	 *	- `action=stopVideoCapture&session=<test_session_id>`: stop video capture previously created (use the provided sessionId)
	 *	- `action=driverPids&browserName=<browser>&browserVersion=<version>&existingPids=<some_pids>`: Returns list of PIDS for this driver exclusively. This allows the hub to know which browser has been recently started. If existingPids is not empty, these pids won't be returned by the command. Browser name and version refers to installed browsers, declared in grid node
	 *	- `action=browserAndDriverPids&browserName=<browser>&browserVersion=<version>&parentPids=<some_pid>`: Returns list of PIDs for this driver and for all subprocess created (driver, browser and other processes). This allows to kill any process created by a driver. parentPids are the processs for which we should search child processes.
	 *	- `action=keepAlive`: move mouse from 1 pixel so that windows session does not lock
	 *  - `action=processList&name=myprocessName`: returns the list of PIDs whose process name is the requested one
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServletResponse response = executeGetAction(req.getParameterMap(), req.getContentType(), req.getInputStream());
		response.send(resp);
	}
	public ServletResponse executeGetAction(Map<String, String[]> parameters, String contentType, InputStream inputStream) {
		switch (parameters.get("action")[0]) {
		case "version":
			return sendVersion();

		case "screenshot":
			return takeScreenshot();
			
		case "startVideoCapture":
			return startVideoCapture(parameters.get("session")[0]);
			
		case "stopVideoCapture":
			return stopVideoCapture(parameters.get("session")[0]);

		case "mouseCoordinates":
			return mouseCoordinates();
			
		case "driverPids":
			String existingPidsStr = parameters.get("existingPids")[0];
			List<Long> existingPids = existingPidsStr.isEmpty() ? new ArrayList<>(): Arrays.asList(existingPidsStr.split(","))
					.stream()
					.map(Long::parseLong)
					.collect(Collectors.toList());
			return getBrowserPids(parameters.get("browserName")[0], parameters.get("browserVersion")[0], existingPids);
			
		case "browserAndDriverPids":
			String parentPidsStr = parameters.get("parentPids")[0];
			List<Long> parentPids = parentPidsStr.isEmpty() ? new ArrayList<>(): Arrays.asList(parentPidsStr.split(","))
					.stream()
					.map(Long::parseLong)
					.collect(Collectors.toList());
			return getAllBrowserSubprocessPids(parameters.get("browserName")[0], parameters.get("browserVersion")[0], parentPids);
			
		case "processList":
			String processName = parameters.get("name")[0];
			return getProcessList(processName);
		
		default:
			return new ServletResponse(HttpServletResponse.SC_NOT_FOUND, String.format("GET Action %s not supported by servlet", parameters.get("action")[0]));
		}
	}
	
	/**
	 * Send a command to the driver to keep it alive
	 * @param session
	 * @throws UnirestException
	 */
	private void keepDriverAlive(String session) throws UnirestException {
		HttpResponse<String> resp = Unirest.get(String.format("%s/session/%s/url", LaunchConfig.getCurrentNodeConfig().getNodeOptions().getPublicGridUri().get(), session))
				.asString();
		logger.info("Staying alive: " + resp.getStatus());
	}
	
	private ServletResponse executeCommand(String commandName, List<String> args, String sessionKey, Integer timeout) {
		
		ExecutorService executorService = null;
		if (sessionKey != null) {
			logger.info("disable timeout during command execution");

			executorService = Executors.newSingleThreadExecutor();
			executorService.submit(() -> {
				while (true) {
					keepDriverAlive(sessionKey);
					try {
						logger.info("waiting");
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
			return new ServletResponse(HttpServletResponse.SC_OK, commandTask.getResult());
		} catch (Exception e) {
			logger.warn("Could not exeecute command: " + e.getMessage(), e);
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			if (sessionKey != null) {
				logger.info("enable timeout after command execution");
				executorService.shutdownNow();
				logger.info("timeout enabled");
			}
		}
	}
	
	private ServletResponse killTask(String taskName) {
		logger.info("killing process " + taskName);
		try {
			assert taskName != null;
			new KillTask().withName(taskName)
				.execute();
			return new ServletResponse(HttpServletResponse.SC_OK, "process killed");
		} catch (Exception e) {
			logger.warn("Could not kill process: " + e.getMessage(), e);
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}	
	}
	
	private ServletResponse killPid(Long pid) {
		logger.info("killing process " + pid);
		try {
			assert pid != null;
			new KillTask().withPid(pid)
				.execute();
			return new ServletResponse(HttpServletResponse.SC_OK, "process killed");
		} catch (Exception e) {
			logger.warn("Could not kill process: " + e.getMessage(), e);
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}	
	} 
	
	/**
	 * Left clic on desktop
	 * @param x
	 * @param y
	 * @throws IOException 
	 */
	private ServletResponse leftClick(boolean onlyMainScreen, int x, int y) {
		try {
			logger.info(String.format("left clic at %d,%d", x, y));
			CustomEventFiringWebDriver.leftClicOnDesktopAt(onlyMainScreen, x, y, DriverMode.LOCAL, null);
			return new ServletResponse(HttpServletResponse.SC_OK, "left clic ok");
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	private ServletResponse mouseCoordinates() {
		try {
			logger.info("mouse coordinates");
			Point coords = CustomEventFiringWebDriver.getMouseCoordinates(DriverMode.LOCAL, null);
			return new ServletResponse(HttpServletResponse.SC_OK, String.format("%d,%d", coords.x, coords.y));
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * double clic on desktop
	 * @param x
	 * @param y
	 * @throws IOException 
	 */
	private ServletResponse doubleClick(boolean onlyMainScreen, int x, int y) {
		try {
			logger.info(String.format("left clic at %d,%d", x, y));
			CustomEventFiringWebDriver.doubleClickOnDesktopAt(onlyMainScreen, x, y, DriverMode.LOCAL, null);
			return new ServletResponse(HttpServletResponse.SC_OK, "double clic ok");
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * right clic on desktop
	 * @param x
	 * @param y
	 * @throws IOException 
	 */
	private ServletResponse rightClick(boolean onlyMainScreen, int x, int y) {
		try {
			logger.info(String.format("right clic at %d,%d", x, y));
			CustomEventFiringWebDriver.rightClicOnDesktopAt(onlyMainScreen, x, y, DriverMode.LOCAL, null);
			return new ServletResponse(HttpServletResponse.SC_OK, "right clic ok");
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * screenshot of the desktop
	 * @throws IOException 
	 */
	private ServletResponse takeScreenshot() {
		logger.info("taking screenshot");
		try  {
			return new ServletResponse(HttpServletResponse.SC_OK, CustomEventFiringWebDriver.captureDesktopToBase64String(DriverMode.LOCAL, null));
        } catch (Exception e) {
        	logger.error("Error sending screenshot", e);
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
	}
	
	/**
	 * Send key events
	 * @param keys
	 * @throws IOException 
	 */
	private ServletResponse sendKeys(List<Integer> keys) {
		try {
			logger.info("sending keys: " + keys);
			CustomEventFiringWebDriver.sendKeysToDesktop(keys, DriverMode.LOCAL, null);
			return new ServletResponse(HttpServletResponse.SC_OK, "send keys");
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * write text using keyboard
	 * @param text
	 * @throws IOException 
	 */
	private ServletResponse writeText(String text) {
		try {
			logger.info("writing text: " + text);
			CustomEventFiringWebDriver.writeToDesktop(text, DriverMode.LOCAL, null);
			return new ServletResponse(HttpServletResponse.SC_OK, "write text ok");
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * write text using keyboard
	 * @param stepName
	 * @param sessionId
	 * @throws IOException
	 */
	private ServletResponse displayRunningStep(String stepName, String sessionId) {
		try {
			logger.info("display step: " + stepName);
			new DisplayRunningStepTask(stepName, sessionId).execute();
			return new ServletResponse(HttpServletResponse.SC_OK, "display step ok");
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * upload file to browser
	 * 
	 * @param fileName		name of the file to upload 
	 * @param fileContent	content as a base64 string
	 * @throws IOException 
	 */
	private ServletResponse uploadFile(String fileName, String fileContent) {
		try {
			logger.info("uploading file: " + fileName);
			CustomEventFiringWebDriver.uploadFile(fileName, fileContent, DriverMode.LOCAL, null);
			return new ServletResponse(HttpServletResponse.SC_OK, "upload file ok");
		} catch (Exception e) {
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	private ServletResponse sendVersion() {
		Map<String, String> version = new HashMap<>();
		version.put("version", Utils.getCurrentversion());
		return new ServletResponse(HttpServletResponse.SC_OK, new JSONObject(version).toString(), MediaType.JSON_UTF_8);
	}
	
	private ServletResponse startVideoCapture(String sessionId) {
		try {
			logger.info("start video capture for session: " + sessionId);
			
			new StartVideoCaptureTask(sessionId).execute();

			logger.info("video capture started");
			return new ServletResponse(HttpServletResponse.SC_OK, "start video ok");
		} catch (Exception e) {
			logger.error("Error starting video capture", e);
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Stop capture, send file and delete it afterwards
	 * @param sessionId
	 * @throws IOException
	 */
	private ServletResponse stopVideoCapture(String sessionId) {
		logger.info("stop video capture for session: " + sessionId);
		try {
			File videoFile = new StopVideoCaptureTask(sessionId)
				.execute()
				.getVideoFile();

			logger.info("video capture stopped");
			if (videoFile != null) {
				return new ServletResponse(HttpServletResponse.SC_OK, videoFile, MediaType.create("video", "x-msvideo"));
			}
			return new ServletResponse(HttpServletResponse.SC_NOT_FOUND, "no video to send");
		} catch (Exception e) {
			logger.error("Error stopping video capture", e);
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Returns list of PID for the given browser
	 * @param browserName
	 * @param browserVersion
	 * @throws IOException 
	 */
	private ServletResponse getBrowserPids(String browserName, String browserVersion, List<Long> existingPids) {
		logger.info("get driver pids for browser " + browserName);
		
		
		try  {
			// get pid pre-existing the creation of this driver. This helps filtering drivers launched by other tests or users
			List<Long> pidsToReturn = new DiscoverBrowserAndDriverPidsTask(browserName, browserVersion)
					.withExistingPids(existingPids)
					.execute()
					.getProcessPids();

			return new ServletResponse(HttpServletResponse.SC_OK, StringUtils.join(pidsToReturn, ","));
        } catch (Exception e) {
        	logger.error("Error sending browser pids", e);
        	return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
	}
	
	/**
	 * Returns list of PIDs corresponding to driver and browser (+ processes that could have been created by browser)
	 * @param browserName
	 * @param browserVersion
	 * @throws IOException 
	 */
	private ServletResponse getAllBrowserSubprocessPids(String browserName, String browserVersion, List<Long> parentPids) {
		logger.info("get browser/driver pids for browser " + browserName);

		try {
			List<Long> subProcessPids = new DiscoverBrowserAndDriverPidsTask(browserName, browserVersion)
					.withParentsPids(parentPids)
					.execute()
					.getProcessPids();

			return new ServletResponse(HttpServletResponse.SC_OK, StringUtils.join(subProcessPids, ","));
		} catch (Exception e) {
			logger.error("Error sending browser/driver pids", e);
			return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	
	
	/**
	 * Send to requester, the list of PIDs whose name is the requested process name
	 * @param processName
	 * @throws IOException 
	 */
	private ServletResponse getProcessList(String processName) {
		
		try  {
			
			List<ProcessInfo> processList = OSUtilityFactory.getInstance().getRunningProcesses(processName);
			List<String> pidsToReturn = processList.stream()
					.map(p -> p.getPid())
					.collect(Collectors.toList());

			return new ServletResponse(HttpServletResponse.SC_OK, StringUtils.join(pidsToReturn, ","));
        } catch (Exception e) {
        	logger.error("Error sending browser pids", e);
        	return new ServletResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
	}

}
