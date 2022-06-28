package com.infotel.seleniumrobot.grid.tasks.video;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.infotel.seleniumrobot.grid.tasks.Task;
import com.seleniumtests.util.video.VideoRecorder;

public abstract class VideoCaptureTask implements Task {

	public static final String VIDEOS_FOLDER = "videos";

	protected static Map<String, VideoRecorder> videoRecorders = Collections.synchronizedMap(new HashMap<>());
	
	public static void resetVideoRecorders() {
		videoRecorders = Collections.synchronizedMap(new HashMap<>());
	}
}
