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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.openqa.grid.internal.GridRegistry;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.EndTask;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tasks.NodeRestartTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.mobile.AppiumLauncher;
import com.seleniumtests.browserfactory.mobile.LocalAppiumLauncher;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.driver.screenshots.VideoRecorder;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.seleniumtests.util.osutility.ProcessInfo;

/**
 * Servlet for getting all mobile devices information
 * This helps the hub to update capabilities with information on the connected device
 * @author behe
 *
 */
public class NodeTaskServlet extends GenericServlet {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 216473127866019518L;

	private static final Logger logger = Logger.getLogger(NodeTaskServlet.class);
	public static final String VIDEOS_FOLDER = "videos";
	private static Map<String, VideoRecorder> videoRecorders = Collections.synchronizedMap(new HashMap<>());
	private static Map<String, File> recordedFiles = Collections.synchronizedMap(new HashMap<>());
	private static Map<String, AppiumLauncher> appiumLaunchers = Collections.synchronizedMap(new HashMap<>());
	
	private NodeRestartTask restartTask = new NodeRestartTask();
	private EndTask endTask = new EndTask();
	private KillTask killTask = new KillTask();
	
	private Object lock = new Object();
	
	public NodeTaskServlet() {
		super(null);
	}
	
	public NodeTaskServlet(GridRegistry registry) {
		super(registry);
	}
	
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
	 * - `action=uploadFile&name=<file_name>&content=<base64_string>` use browser to upload a file when a upload file window is displayed. The base64 content is copied to a temps file which will then be read by browser.
	 * - `action=setProperty&key=<key>&value=<value>` set java property for the node
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		switch (req.getParameter("action")) {
		case "restart":
			restartNode();
			break;
			
		case "stop":
			sendOk(resp);
			stopNode();
			break;
			
		// call POST /extra/NodeTaskServlet/kill with process=<task_name>
		case "kill":
			synchronized (lock) {
				killTask(req.getParameter("process"));
			}
			break;
			
		case "killPid":
			synchronized (lock) {
				killPid(Long.parseLong(req.getParameter("pid")));
			}
			break;
			
		// call POST /extra/NodeTaskServlet/leftClic with x=<x-coordinate>,y=<y_coordinate>
		case "leftClic":
		case "leftClick":
			leftClick(Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")));
			break;
			
			// call POST /extra/NodeTaskServlet/doubleClic with x=<x-coordinate>,y=<y_coordinate>
		case "doubleClick":
			doubleClick(Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")));
			break;
			
		// call POST /extra/NodeTaskServlet/rightClic with x=<x-coordinate>,y=<y_coordinate>
		case "rightClic":
		case "rightClick":
			rightClick(Integer.parseInt(req.getParameter("x")), Integer.parseInt(req.getParameter("y")));
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
			
		case "setProperty":
			System.setProperty(req.getParameter("key"), req.getParameter("value"));
			break;
			
		case "clean":
			cleanNode();
			break;
			
		default:
			sendError(resp, String.format("POST Action %s not supported by servlet", req.getParameter("action")));
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
			startVideoCapture(req.getParameter("session"));
			break;
			
		case "stopVideoCapture":
			stopVideoCapture(req.getParameter("session"), resp);
			break;
			
		case "startAppium":
			startAppium(req.getParameter("session"), resp);
			break;
			
		case "stopAppium":
			stopAppium(req.getParameter("session"));
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
			
		case "keepAlive":
			keepAlive();
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
	
	private void killPid(Long pid) {
		logger.info("killing process " + pid);
		try {
			assert pid != null;
			killTask.setTaskPid(pid);
			killTask.execute();
		} catch (Exception e) {
			logger.warn("Could not kill process: " + e.getMessage(), e);
		}	
	}
	
	private void stopNode() {
		logger.info("stopping");
		try {
			endTask.execute();
		} catch (Exception e) {
			logger.warn("Could not stop node: " + e.getMessage(), e);
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
	 * Kill drivers and browsers
	 * clean temp folder
	 */
	private void cleanNode() {

		// delete video files older than 8 hours
		try {
			Files.walk(Paths.get(Utils.getRootdir(), VIDEOS_FOLDER))
			        .filter(Files::isRegularFile)
			        .filter(p -> p.toFile().lastModified() < Instant.now().minusSeconds(8 * 3600).toEpochMilli())
			        .forEach(t -> {
						try {
							String name = t.getFileName().toString();
							recordedFiles.remove(name);
							Files.delete(t);
						} catch (IOException e) {}
					});
			
		} catch (IOException e) {
		}
		
		// do not clear drivers and browser when devMode is true
		if (LaunchConfig.getCurrentLaunchConfig().getDevMode()) {
			return;
		}
		
		try {
			OSUtilityFactory.getInstance().killAllWebBrowserProcess(true);
			OSUtilityFactory.getInstance().killAllWebDriverProcess();
		} catch (Exception e) {
			logger.error("Error while kill browser / drivers", e);
		}
		
		File temp;
		try {
			temp = File.createTempFile("temp-file-name", ".tmp");
			File tempFolder = temp.getParentFile().getAbsoluteFile();
			FileUtils.cleanDirectory(tempFolder);
		} catch (IOException e) {
		} 	
		
		// kill popup raised on windows when a driver crashes on Windows
		if (OSUtility.isWindows()) {
			try {
				OSUtilityFactory.getInstance().killProcessByName("Werfault", true);
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * Left clic on desktop
	 * @param x
	 * @param y
	 */
	private void leftClick(int x, int y) {
		logger.info(String.format("left clic at %d,%d", x, y));
		CustomEventFiringWebDriver.leftClicOnDesktopAt(x, y, DriverMode.LOCAL, null);
	}
	
	/**
	 * double clic on desktop
	 * @param x
	 * @param y
	 */
	private void doubleClick(int x, int y) {
		logger.info(String.format("left clic at %d,%d", x, y));
		CustomEventFiringWebDriver.doubleClickOnDesktopAt(x, y, DriverMode.LOCAL, null);
	}
	
	/**
	 * right clic on desktop
	 * @param x
	 * @param y
	 */
	private void rightClick(int x, int y) {
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
			outputStream.print(CustomEventFiringWebDriver.captureDesktopToBase64String(DriverMode.LOCAL, null));
			outputStream.flush();
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
			outputStream.print(new JSONObject(version).toString());
        } catch (IOException e) {
        	logger.error("Error sending reply", e);
        }
	}
	
	private void startVideoCapture(String sessionId) {
		logger.info("start video capture for session: " + sessionId);
		String videoName = sessionId + ".avi";
		VideoRecorder recorder = CustomEventFiringWebDriver.startVideoCapture(DriverMode.LOCAL, null, Paths.get(Utils.getRootdir(), VIDEOS_FOLDER).toFile(), videoName);
		videoRecorders.put(sessionId, recorder);
	}
	
	/**
	 * Stop capture, send file and delete it afterwards
	 * @param sessionId
	 * @param resp
	 * @throws IOException
	 */
	private void stopVideoCapture(String sessionId, HttpServletResponse resp) throws IOException {
		logger.info("stop video capture for session: " + sessionId);
		VideoRecorder recorder = videoRecorders.remove(sessionId);
		File knownFile = recordedFiles.get(sessionId);
		
		File videoFile;
		if (recorder == null) {
			if (knownFile == null) {
				return;
			} else {
				videoFile = knownFile;
			}
		} else {
			videoFile = CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder);
		}
		
		if (videoFile != null) {
			recordedFiles.put(sessionId, videoFile);
			try (
	            ServletOutputStream outputStream = resp.getOutputStream()) {
				outputStream.write(FileUtils.readFileToByteArray(videoFile));
				outputStream.flush();
	        } catch (IOException e) {
	        	logger.error("Error sending reply", e);
	        } 
		}
		logger.info("video capture stopped");
	}
	
	private void sendOk(HttpServletResponse resp) throws IOException {
		try {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getOutputStream().print("OK");
		} catch (Exception e) {
			e.printStackTrace();
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

	public static Map<String, VideoRecorder> getVideoRecorders() {
		return videoRecorders;
	}
	
	private void startAppium(String sessionId, HttpServletResponse resp) {
		logger.info("starting appium");
		String logDir = Paths.get(Utils.getRootdir(), "logs", "appium").toString();

		// start appium before creating instance
		AppiumLauncher appiumLauncher = new LocalAppiumLauncher(logDir);
    	appiumLauncher.startAppium();
    	appiumLaunchers.put(sessionId, appiumLauncher);
    	
    	logger.info("appium is running on " + ((LocalAppiumLauncher)appiumLauncher).getAppiumServerUrl());
    	
    	try (
            ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(((LocalAppiumLauncher)appiumLauncher).getAppiumServerUrl());
			outputStream.flush();
        } catch (IOException e) {
        	logger.error("Error sending appium URL", e);
        }
	}
	
	private void stopAppium(String sessionId) {
		AppiumLauncher appiumLauncher = appiumLaunchers.remove(sessionId);
		if (appiumLauncher != null) {
			appiumLauncher.stopAppium();
		}
	}
	
	/**
	 * Returns list of PID for the given browser
	 * @param browserType
	 * @param browserVersion
	 * @param resp
	 */
	private void getBrowserPids(String browserName, String browserVersion, List<Long> existingPids, HttpServletResponse resp) {
		logger.info("get driver pids for browser " + browserName);
		BrowserInfo browserInfo = getBrowserInfo(browserName, browserVersion);
    	
    	List<Long> pidsToReturn = new ArrayList<>();

		// get pid pre-existing the creation of this driver. This helps filtering drivers launched by other tests or users
		if (browserInfo != null) {
			pidsToReturn.addAll(browserInfo.getDriverAndBrowserPid(existingPids));
    	}
		
		try (
            ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(StringUtils.join(pidsToReturn, ","));
			outputStream.flush();
        } catch (IOException e) {
        	logger.error("Error sending browser pids", e);
        }
	}
	
	/**
	 * Returns list of PIDs corresponding to driver and browser (+ processes that could have been created by browser)
	 * @param browserType
	 * @param browserVersion
	 * @param resp
	 */
	private void getAllBrowserSubprocessPids(String browserName, String browserVersion, List<Long> parentPids, HttpServletResponse resp) {
		logger.info("get browser/driver pids for browser " + browserName);
		BrowserInfo browserInfo = getBrowserInfo(browserName, browserVersion);
		
		List<Long> subProcessPids = new ArrayList<>();
		if (browserInfo != null) {
			subProcessPids.addAll(browserInfo.getAllBrowserSubprocessPids(parentPids));
		}
		
		try (
			ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(StringUtils.join(subProcessPids, ","));
			outputStream.flush();
		} catch (IOException e) {
			logger.error("Error sending browser/driver pids", e);
		}
	}
	
	/**
	 * Returns BrowserInfo corresponding to this browser name and version
	 * @param browserName
	 * @param browserVersion
	 * @return
	 */
	private BrowserInfo getBrowserInfo(String browserName, String browserVersion) {
		List<BrowserInfo> browserInfos = OSUtility.getInstalledBrowsersWithVersion().get( 
				com.seleniumtests.driver.BrowserType.getBrowserTypeFromSeleniumBrowserType(browserName));
		
		// select the right browserInfo depending on browser version
		BrowserInfo browserInfo = null;
		for (BrowserInfo bi: browserInfos) {
			browserInfo = bi; // get at least one of the browserInfo
			if (bi.getVersion().equals(browserVersion)) {
				break;
			}
		}
		return browserInfo;
	}
	
	/**
	 * Send to requester, the list of PIDs whose name is the requested process name
	 * @param processName
	 */
	private void getProcessList(String processName, HttpServletResponse resp) {
		List<ProcessInfo> processList = OSUtilityFactory.getInstance().getRunningProcesses(processName);
		List<String> pidsToReturn = processList.stream()
				.map(ProcessInfo::getPid)
				.collect(Collectors.toList());
		
		try (
            ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(StringUtils.join(pidsToReturn, ","));
			outputStream.flush();
        } catch (IOException e) {
        	logger.error("Error sending browser pids", e);
        }
	}
	
	private void keepAlive() {

		// do not clear drivers and browser when devMode is true
		if (!LaunchConfig.getCurrentLaunchConfig().getDevMode()) {
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
}
