package com.infotel.seleniumrobot.grid.tasks.video;

import java.nio.file.Paths;

import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.video.VideoRecorder;

public class StartVideoCaptureTask extends VideoCaptureTask {

	private String sessionId;
	
	public StartVideoCaptureTask(String sessionId) {
		this.sessionId = sessionId;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public StartVideoCaptureTask execute() throws Exception {

		String videoName = sessionId + ".avi";
		VideoRecorder recorder = CustomEventFiringWebDriver.startVideoCapture(DriverMode.LOCAL, null, Paths.get(Utils.getRootdir(), VIDEOS_FOLDER).toFile(), videoName);
		videoRecorders.put(sessionId, recorder);
		
		return this;
	}

}
