package com.infotel.seleniumrobot.grid.tasks;

import com.seleniumtests.util.osutility.OSUtilityFactory;

public class KillTask implements Task {
	
	private String taskName;
	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	@Override
	public void execute() {
		OSUtilityFactory.getInstance().killProcessByName(taskName, true);
	}

}
