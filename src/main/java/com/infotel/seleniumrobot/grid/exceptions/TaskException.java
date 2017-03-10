package com.infotel.seleniumrobot.grid.exceptions;

public class TaskException extends RuntimeException {
	public TaskException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    public TaskException(final String message) {
    	super(message);
    }
}
