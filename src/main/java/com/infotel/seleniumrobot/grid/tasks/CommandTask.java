package com.infotel.seleniumrobot.grid.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;

public class CommandTask implements Task {
	
	private static final Logger logger = Logger.getLogger(NodeTaskServlet.class);
	private static final List<String> WINDOWS_COMMAND_WHITE_LIST = Arrays.asList("echo", "cmdkey.exe");
	private static final List<String> LINUX_COMMAND_WHITE_LIST = Arrays.asList("echo");
	private static final List<String> MAC_COMMAND_WHITE_LIST = Arrays.asList("echo");
	
	private String command = "";
	private List<String> args = new ArrayList<String>();
	
	public void setCommand(String command, List<String> args) {
		this.command = command;
		this.args = args;
	}
	
	@Override
	public void execute() {
		if (command == null || command.isEmpty()) {
			logger.error("No command provided");
		} else if (OSUtility.isLinux() && LINUX_COMMAND_WHITE_LIST.contains(command)
					|| OSUtility.isWindows() && WINDOWS_COMMAND_WHITE_LIST.contains(command)
					|| OSUtility.isMac() && MAC_COMMAND_WHITE_LIST.contains(command)) {
			logger.error(String.format("Executing command %s", command));
			args.add(0, command);
			OSCommand.executeCommandAndWait(args.toArray(new String[] {}));
		} else {
			logger.error(String.format("Command %s is not supported", command));
		}
		
	}

}
