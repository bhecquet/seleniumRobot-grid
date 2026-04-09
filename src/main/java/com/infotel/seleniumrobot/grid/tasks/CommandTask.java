package com.infotel.seleniumrobot.grid.tasks;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.seleniumtests.util.osutility.OSCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CommandTask implements Task {

    private static final Logger logger = LogManager.getLogger(CommandTask.class);
    private static final int DEFAULT_TIMEOUT = 30;

    private String command = "";
    private String result = "";
    private int timeout = 30;
    private List<String> args = new ArrayList<>();

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

    @SuppressWarnings("unchecked")
    @Override
    public CommandTask execute() {
        result = "";

        if (command == null || command.isEmpty()) {
            throw new TaskException("No command provided");
        }

        String realCommand = command.replace(OSCommand.USE_PATH, "");

        if (LaunchConfig.getCurrentLaunchConfig().getExternalProgramWhiteList().contains(realCommand)) {
            logger.error("Executing command {}", realCommand);
            args.addFirst(command);
            if (timeout >= 0) {
                result = OSCommand.executeCommandAndWait(args.toArray(new String[]{}), timeout, null);
            } else {
                result = OSCommand.executeCommand(args.toArray(new String[]{})).toString();
            }
        } else {
            throw new TaskException(String.format("Command %s is not supported", command));
        }

        return this;
    }

    public String getResult() {
        return result;
    }

}
