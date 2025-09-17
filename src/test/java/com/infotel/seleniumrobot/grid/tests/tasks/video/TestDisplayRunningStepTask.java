package com.infotel.seleniumrobot.grid.tests.tasks.video;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.video.DisplayRunningStepTask;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.video.VideoRecorder;

public class TestDisplayRunningStepTask extends BaseMockitoTest {

	@Mock
	private VideoRecorder recorder;

	private MockedStatic mockedCustomWebDriver;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		mockedCustomWebDriver = Mockito.mockStatic(CustomEventFiringWebDriver.class);
		new LaunchConfig(new String[] {"node"});
		VideoCaptureTask.resetVideoRecorders();

	}

	@AfterMethod(groups={"grid"}, alwaysRun = true)
	private void closeMocks() {
		mockedCustomWebDriver.close();
	}
	
	@Test(groups= {"grid"})
	public void testDisplayRunningStep() throws Exception {
		VideoCaptureTask.addVideoRecorder("1234", recorder);
		DisplayRunningStepTask task = new DisplayRunningStepTask("step", "1234").execute();

		mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.displayStepOnScreen("step", DriverMode.LOCAL, null, recorder));
		
	}
	
	@Test(groups= {"grid"})
	public void testDisplayRunningStepVideoNotStarted() throws Exception {
		DisplayRunningStepTask task = new DisplayRunningStepTask("step", "1234").execute();

		mockedCustomWebDriver.verify(() -> CustomEventFiringWebDriver.displayStepOnScreen("step", DriverMode.LOCAL, null, null));
		
	}
}
