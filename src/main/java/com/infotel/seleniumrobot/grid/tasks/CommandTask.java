package com.infotel.seleniumrobot.grid.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.seleniumtests.util.osutility.OSCommand;

public class CommandTask implements Task {
	
	private static final Logger logger = Logger.getLogger(NodeTaskServlet.class);
	private static final int DEFAULT_TIMEOUT = 30;
	
	private String command = "";
	private String result = "";
	private int timeout = 30;
	private List<String> args = new ArrayList<String>();
	
	public static CommandTask getInstance() {
		return new CommandTask();

	}
	
	public void setCommand(String command, List<String> args) {
		setCommand(command, args, null);
	}
	
	public void setCommand(String command, List<String> args, Integer timeout) {
		this.command = command;
		this.args = args;
		if (timeout == null) {
			this.timeout = DEFAULT_TIMEOUT;
		} else {
			this.timeout = timeout;
		}
	}
	
	@Override
	public void execute() {
		result = "";
		if (command == null || command.isEmpty()) {
			throw new TaskException("No command provided");
		} else if (LaunchConfig.getCurrentLaunchConfig().getExternalProgramWhiteList().contains(command)) {
			logger.error(String.format("Executing command %s", command));
			args.add(0, command);
			result = OSCommand.executeCommandAndWait(args.toArray(new String[] {}), timeout, null);
		} else {
			throw new TaskException(String.format("Command %s is not supported", command));
		}
		
	}

	public String getResult() {
		return result;
	}

}
