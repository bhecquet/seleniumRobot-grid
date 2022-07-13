package com.infotel.seleniumrobot.grid.tests.tasks.video;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;
import java.nio.file.Paths;

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.video.DisplayRunningStepTask;
import com.infotel.seleniumrobot.grid.tasks.video.StartVideoCaptureTask;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.video.VideoRecorder;

@PrepareForTest({CustomEventFiringWebDriver.class})
public class TestStartVideoCaptureTask extends BaseMockitoTest {

	@Mock
	private VideoRecorder recorder;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
		new LaunchConfig(new String[] {"node"});
		VideoCaptureTask.resetVideoRecorders();

	}
	
	@Test(groups= {"grid"})
	public void testStartVideo() throws Exception {
		PowerMockito.when(CustomEventFiringWebDriver.startVideoCapture(any(DriverMode.class), isNull(), any(File.class), anyString() )).thenReturn(recorder);
		
		new StartVideoCaptureTask("1234").execute();
		PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
		
		CustomEventFiringWebDriver.startVideoCapture(DriverMode.LOCAL, null, Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER).toFile(), "1234.avi");
		Assert.assertTrue(VideoCaptureTask.getVideoRecorders().containsKey("1234"));
		Assert.assertEquals(VideoCaptureTask.getVideoRecorders().get("1234"), recorder);
	}

}
