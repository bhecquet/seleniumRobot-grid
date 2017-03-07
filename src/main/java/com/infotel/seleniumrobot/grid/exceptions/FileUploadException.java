package com.infotel.seleniumrobot.grid.exceptions;

public class FileUploadException extends RuntimeException {

	public FileUploadException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    public FileUploadException(final String message) {
    	super(message);
    }
}
