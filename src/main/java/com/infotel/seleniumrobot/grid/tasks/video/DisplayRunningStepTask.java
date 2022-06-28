package com.infotel.seleniumrobot.grid.tasks.video;

import com.infotel.seleniumrobot.grid.tasks.Task;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.DriverMode;

public class DisplayRunningStepTask extends VideoCaptureTask implements Task {

	private String stepName;
	private String sessionId;
	
	public DisplayRunningStepTask(String stepName, String sessionId) {
		this.stepName = stepName;
		this.sessionId = sessionId;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DisplayRunningStepTask execute() throws Exception {
		CustomEventFiringWebDriver.displayStepOnScreen(stepName, DriverMode.LOCAL, null, videoRecorders.get(sessionId));
		return this;
	}

}
