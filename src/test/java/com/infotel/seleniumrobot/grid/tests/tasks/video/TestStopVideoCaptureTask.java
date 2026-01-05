package com.infotel.seleniumrobot.grid.tests.tasks.video;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.video.StopVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.video.VideoRecorder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

public class TestStopVideoCaptureTask extends BaseMockitoTest {

    @Mock
    private VideoRecorder recorder;

    @BeforeMethod(groups = {"grid"})
    private void setup() {

        new LaunchConfig(new String[]{"node"});
        VideoCaptureTask.resetVideoRecorders();

    }

    @Test(groups = {"grid"})
    public void testStopVideoCapture() throws Exception {
        try (MockedStatic<CustomEventFiringWebDriver> mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
            // simulate a running recording
            StopVideoCaptureTask.addVideoRecorder("1234", recorder);

            mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));

            StopVideoCaptureTask task = new StopVideoCaptureTask("1234").execute();
            Assert.assertEquals(task.getVideoFile(), new File("video.avi"));

            mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder));

            // recorder has been removed
            Assert.assertFalse(VideoCaptureTask.getVideoRecorders().containsKey("1234"));

            // a file has been added
            Assert.assertTrue(StopVideoCaptureTask.getRecordedFiles().containsKey("1234"));
            Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().get("1234"), new File("video.avi"));
        }
    }

    @Test(groups = {"grid"})
    public void testStopVideoCaptureSeveralTimes() throws Exception {

        File videoFile = Files.createTempFile("video", ".avi").toFile();

        // simulate a running recording
        StopVideoCaptureTask.addVideoRecorder("1234", recorder);

        // simulate delay in video handling
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Map<Integer, File> recordedFiles = new HashMap<>();

        // first call
        executorService.submit(() -> {

            StopVideoCaptureTask task = null;
            try (MockedStatic<CustomEventFiringWebDriver> mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
                mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class)))
                        .thenAnswer(invocation -> {
                            WaitHelper.waitForSeconds(4);
                            return videoFile;
                        });
                task = new StopVideoCaptureTask("1234").execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            recordedFiles.put(1, task.getVideoFile());
        });

        // stop a second time
        WaitHelper.waitForSeconds(1);
        executorService.submit(() -> {
            StopVideoCaptureTask task = null;
            try (MockedStatic<CustomEventFiringWebDriver> mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
                mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(videoFile);

                task = new StopVideoCaptureTask("1234").execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            recordedFiles.put(2, task.getVideoFile());
        });

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
        // recorder has been removed
        Assert.assertFalse(VideoCaptureTask.getVideoRecorders().containsKey("1234"));

        // a file has been added
        Assert.assertTrue(StopVideoCaptureTask.getRecordedFiles().containsKey("1234"));
        Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().get("1234"), videoFile);

        // check each call get the video file
        Assert.assertEquals(recordedFiles.getOrDefault(1, null), videoFile, "Video file not present");
        Assert.assertEquals(recordedFiles.getOrDefault(2, null), videoFile, "Video file2 not present");

    }

    /**
     * If we call stop several times, when recording is stopped, we get the stored file
     */
    @Test(groups = {"grid"})
    public void testStopVideoCaptureNoRecorder() throws Exception {
        try (MockedStatic<CustomEventFiringWebDriver> mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
            File videoFile = File.createTempFile("video", ".avi");
            videoFile.deleteOnExit();

            Map<String, File> recordedFiles = new HashMap<>();
            recordedFiles.put("1234", videoFile);

            // simulate a ended recording (no recorder set)
            StopVideoCaptureTask.setRecordedFiles(recordedFiles);

            mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));

            StopVideoCaptureTask task = new StopVideoCaptureTask("1234").execute();
            Assert.assertEquals(task.getVideoFile(), videoFile);

            mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder), never());

            // file has been kept
            Assert.assertTrue(StopVideoCaptureTask.getRecordedFiles().containsKey("1234"));
            Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().get("1234"), videoFile);
        }
    }

    @Test(groups = {"grid"})
    public void testStopVideoCaptureNoKnownFile() throws Exception {
        try (MockedStatic<CustomEventFiringWebDriver> mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
            mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));

            StopVideoCaptureTask task = new StopVideoCaptureTask("1234").execute();
            Assert.assertNull(task.getVideoFile());

            mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder), never());

            // file has been kept
            Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().size(), 0);
        }
    }

    /**
     * A file exists, check it's removed as it does not exist on file system
     */
    @Test(groups = {"grid"})
    public void testStopVideoCaptureExistingFile() throws Exception {
        try (MockedStatic<CustomEventFiringWebDriver> mockedCustomWebDriver = mockStatic(CustomEventFiringWebDriver.class)) {
            // add 2 files to check for ConcurrentModificationException
            Map<String, File> recordedFiles = new HashMap<>();
            recordedFiles.put("bar", new File("oldVideo.avi"));
            recordedFiles.put("foo", new File("oldVideo2.avi"));

            // simulate a running recording
            StopVideoCaptureTask.addVideoRecorder("1234", recorder);
            StopVideoCaptureTask.setRecordedFiles(recordedFiles);

            mockedCustomWebDriver.when(() -> CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));

            StopVideoCaptureTask task = new StopVideoCaptureTask("1234").execute();
            Assert.assertEquals(task.getVideoFile(), new File("video.avi"));

            // old file has been removed
            Assert.assertFalse(StopVideoCaptureTask.getRecordedFiles().containsKey("foo"));
            Assert.assertFalse(StopVideoCaptureTask.getRecordedFiles().containsKey("bar"));

            // a file has been added
            Assert.assertTrue(StopVideoCaptureTask.getRecordedFiles().containsKey("1234"));
            Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().get("1234"), new File("video.avi"));
        }
    }

}
