package com.infotel.seleniumrobot.grid.tests.tasks.video;

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.video.DisplayRunningStepTask;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.video.VideoRecorder;

@PrepareForTest({CustomEventFiringWebDriver.class})
public class TestDisplayRunningStepTask extends BaseMockitoTest {

	@Mock
	private VideoRecorder recorder;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		PowerMockito.mockStatic(CustomEventFiringWebDriver.class);
		new LaunchConfig(new String[] {"node"});
		VideoCaptureTask.resetVideoRecorders();

	}
	
	@Test(groups= {"grid"})
	public void testDisplayRunningStep() throws Exception {
		VideoCaptureTask.addVideoRecorder("1234", recorder);
		DisplayRunningStepTask task = new DisplayRunningStepTask("step", "1234").execute();
		PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
		CustomEventFiringWebDriver.displayStepOnScreen("step", DriverMode.LOCAL, null, recorder);
		
	}
	
	@Test(groups= {"grid"})
	public void testDisplayRunningStepVideoNotStarted() throws Exception {
		DisplayRunningStepTask task = new DisplayRunningStepTask("step", "1234").execute();
		PowerMockito.verifyStatic(CustomEventFiringWebDriver.class);
		CustomEventFiringWebDriver.displayStepOnScreen("step", DriverMode.LOCAL, null, null);
		
	}
}
