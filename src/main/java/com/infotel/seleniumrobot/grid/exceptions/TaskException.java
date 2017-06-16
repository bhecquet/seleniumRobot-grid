package com.infotel.seleniumrobot.grid.exceptions;

public class TaskException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public TaskException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    public TaskException(final String message) {
    	super(message);
    }
}
