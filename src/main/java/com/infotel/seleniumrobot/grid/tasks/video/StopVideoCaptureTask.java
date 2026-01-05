package com.infotel.seleniumrobot.grid.tasks.video;

import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.video.VideoRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StopVideoCaptureTask extends VideoCaptureTask {

    private static final Object lock = new Object();
    private static final Logger logger = LogManager.getLogger(StopVideoCaptureTask.class);

    private static Map<String, File> recordedFiles = Collections.synchronizedMap(new HashMap<>());
    private final String sessionId;
    private File videoFile;

    public StopVideoCaptureTask(String sessionId) {
        this.sessionId = sessionId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public StopVideoCaptureTask execute() throws Exception {
        synchronized (lock) {
            logger.info("stop video capture for session: {}", sessionId.replaceAll("[\n\r]", "_"));

            // check if some files have been cleaned and delete their reference
            List<String> keysToRemove;

            keysToRemove = recordedFiles
                    .entrySet()
                    .stream()
                    .filter(entry -> !entry.getValue().exists())
                    .map(e -> e.getKey()).collect(Collectors.toList());

            for (String name : keysToRemove) {
                recordedFiles.remove(name);
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
        }
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
