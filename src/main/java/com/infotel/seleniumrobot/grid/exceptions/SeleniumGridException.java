package com.infotel.seleniumrobot.grid.exceptions;

public class SeleniumGridException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	public SeleniumGridException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    public SeleniumGridException(final String message) {
    	super(message);
    }
}
