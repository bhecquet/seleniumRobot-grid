package com.infotel.seleniumrobot.grid.exceptions;

public class FileUploadException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public FileUploadException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    public FileUploadException(final String message) {
    	super(message);
    }
}
