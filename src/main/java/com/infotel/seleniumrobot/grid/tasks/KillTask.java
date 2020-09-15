package com.infotel.seleniumrobot.grid.tasks;

import com.seleniumtests.util.osutility.OSUtilityFactory;

public class KillTask implements Task {
	
	private String taskName;
	private Long taskPid;
	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public void setTaskPid(Long taskPid) {
		this.taskPid = taskPid;
	}

	@Override
	public void execute() throws Exception {
		if (taskName != null) {
			OSUtilityFactory.getInstance().killProcessByName(taskName, true);
		} else if (taskPid != null) {
			OSUtilityFactory.getInstance().killProcess(taskPid.toString(), true);
		}
		taskName = null;
		taskPid = null;
	}

}
