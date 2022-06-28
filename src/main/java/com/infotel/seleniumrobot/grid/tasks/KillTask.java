package com.infotel.seleniumrobot.grid.tasks;

import com.seleniumtests.util.osutility.OSUtilityFactory;

public class KillTask implements Task {
	
	private String taskName;
	private Long taskPid;
	
	public KillTask withName(String taskName) {
		this.taskName = taskName;
		return this;
	}

	public KillTask withPid(Long taskPid) {
		this.taskPid = taskPid;
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public KillTask execute() throws Exception {
		if (taskName != null) {
			OSUtilityFactory.getInstance().killProcessByName(taskName, true);
		} else if (taskPid != null) {
			OSUtilityFactory.getInstance().killProcess(taskPid.toString(), true);
		}
		taskName = null;
		taskPid = null;

		return this;
	}

}
