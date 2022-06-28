package com.infotel.seleniumrobot.grid.tasks;

public interface Task {

	public <T extends Task> T execute() throws Exception;
}
