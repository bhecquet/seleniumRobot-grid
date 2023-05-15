package com.infotel.seleniumrobot.grid.tests.tasks.video;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.video.StopVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.video.VideoRecorder;

@PrepareForTest({CustomEventFiringWebDriver.class})
public class TestStopVideoCaptureTask extends BaseMockitoTest {

	@Mock
	private VideoRecorder recorder;
	
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
		new LaunchConfig(new String[] {"node"});
		VideoCaptureTask.resetVideoRecorders();

	}
	
	@Test(groups= {"grid"})
	public void testStopVideoCapture() throws Exception {
		// simulate a running recording
		StopVideoCaptureTask.addVideoRecorder("1234", recorder);
		
		PowerMockito.when(CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));
		
		StopVideoCaptureTask task = new StopVideoCaptureTask("1234").execute();
		Assert.assertEquals(task.getVideoFile(), new File("video.avi"));
		
		PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
		CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder);
		
		// recorder has been removed
		Assert.assertFalse(VideoCaptureTask.getVideoRecorders().containsKey("1234"));
		
		// a file has been added
		Assert.assertTrue(StopVideoCaptureTask.getRecordedFiles().containsKey("1234"));
		Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().get("1234"), new File("video.avi"));
	}
	
	/**
	 * If we call stop several times, when recording is stopped, we get the stored file
	 * @throws Exception
	 */
	@Test(groups= {"grid"})
	public void testStopVideoCaptureNoRecorder() throws Exception {

		File videoFile = File.createTempFile("video", ".avi");
		videoFile.deleteOnExit();
		
		Map<String, File> recordedFiles = new HashMap<>();
		recordedFiles.put("1234", videoFile);
		
		// simulate a ended recording (no recorder set)
		StopVideoCaptureTask.setRecordedFiles(recordedFiles);
		
		PowerMockito.when(CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));
		
		StopVideoCaptureTask task = new StopVideoCaptureTask("1234").execute();
		Assert.assertEquals(task.getVideoFile(), videoFile);
		
		PowerMockito.verifyStatic(CustomEventFiringWebDriver.class, never());
		CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder);

		// file has been kept
		Assert.assertTrue(StopVideoCaptureTask.getRecordedFiles().containsKey("1234"));
		Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().get("1234"), videoFile);
	}
	
	@Test(groups= {"grid"})
	public void testStopVideoCaptureNoKnownFile() throws Exception {
		
		PowerMockito.when(CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));
		
		StopVideoCaptureTask task = new StopVideoCaptureTask("1234").execute();
		Assert.assertNull(task.getVideoFile());
		
		PowerMockito.verifyStatic(CustomEventFiringWebDriver.class, never());
		CustomEventFiringWebDriver.stopVideoCapture(DriverMode.LOCAL, null, recorder);
		
		// file has been kept
		Assert.assertEquals(StopVideoCaptureTask.getRecordedFiles().size(), 0);
	}
	
	/**
	 * A file exists, check it's removed as it does not exist
	 * @throws Exception
	 */
	@Test(groups= {"grid"})
	public void testStopVideoCaptureExistingFile() throws Exception {
		
		// add 2 files to check for ConcurrentModificationException
		Map<String, File> recordedFiles = new HashMap<>();
		recordedFiles.put("bar", new File("oldVideo.avi"));
		recordedFiles.put("foo", new File("oldVideo2.avi"));
		
		// simulate a running recording
		StopVideoCaptureTask.addVideoRecorder("1234", recorder);
		StopVideoCaptureTask.setRecordedFiles(recordedFiles);
		
		PowerMockito.when(CustomEventFiringWebDriver.stopVideoCapture(any(DriverMode.class), isNull(), any(VideoRecorder.class))).thenReturn(new File("video.avi"));
		
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
