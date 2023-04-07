package com.infotel.seleniumrobot.grid.tasks.video;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.video.VideoRecorder;

public class StopVideoCaptureTask extends VideoCaptureTask {
	
	private static final Logger logger = LogManager.getLogger(StopVideoCaptureTask.class);

	private static Map<String, File> recordedFiles = Collections.synchronizedMap(new HashMap<>());
	private String sessionId;
	private File videoFile;
	
	public StopVideoCaptureTask(String sessionId) {
		this.sessionId = sessionId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public StopVideoCaptureTask execute() throws Exception {
		logger.info("stop video capture for session: " + sessionId);
			
		// check if some files have been cleaned
		synchronized(recordedFiles) {
			for (String name: recordedFiles.keySet()) {
				if (!recordedFiles.get(name).exists()) { 
					recordedFiles.remove(name);
				}
			}
		}
		
		VideoRecorder recorder = videoRecorders.remove(sessionId);
		File knownFile = recordedFiles.get(sessionId);
		
		if (recorder == null) {
			if (knownFile == null) {
				return this;
			} else {
				videoFile = knownFile;
			}
		} else {
			videoFile = CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder);
		}
		
		if (videoFile != null) {
			recordedFiles.put(sessionId, videoFile);
		}
		logger.info("video capture stopped");

		return this;
	}
	
	public File getVideoFile() {
		return videoFile;
	}

	public static Map<String, File> getRecordedFiles() {
		return recordedFiles;
	}

	public static void setRecordedFiles(Map<String, File> recordedFiles) {
		StopVideoCaptureTask.recordedFiles = recordedFiles;
	}

}
