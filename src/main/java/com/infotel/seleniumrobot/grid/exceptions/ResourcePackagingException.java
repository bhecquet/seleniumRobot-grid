package com.infotel.seleniumrobot.grid.exceptions;

/**
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 25/09/2015
 */
public class ResourcePackagingException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ResourcePackagingException(String message) {
        super(message);
    }

    public ResourcePackagingException(Throwable cause) {
        super(cause);
    }
}
