package com.infotel.seleniumrobot.grid.tasks;

import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.osutility.OSUtilityFactory;

public class NodeRestartTask {

	/**
	 * Kills the current process
	 */
	public void execute() {
		
		
		
		Long pid = Utils.getCurrentPID();
		
		OSUtilityFactory.getInstance().killProcess(pid.toString(), true);
	}
	
	
}
