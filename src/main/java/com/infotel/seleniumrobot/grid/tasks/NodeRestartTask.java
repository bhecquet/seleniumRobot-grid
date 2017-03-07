package com.infotel.seleniumrobot.grid.tasks;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtilityFactory;

public class NodeRestartTask {
	
	private static final Logger logger = Logger.getLogger(NodeRestartTask.class);

	/**
	 * Kills the current process
	 */
	public void execute() {
		
		// start the new version
		String command;
		if (SystemUtils.IS_OS_WINDOWS) {
			command = "launch.bat";
		} else {
			command = "launch.sh";
		}
		
		command += String.join(" ", LaunchConfig.getCurrentLaunchConfig().getOriginalArgs());
		logger.info("execute command: " + command);
		OSCommand.executeCommand(command);
		
		// kill current
		Long pid = Utils.getCurrentPID();
		OSUtilityFactory.getInstance().killProcess(pid.toString(), true);
	}
	
	
}
