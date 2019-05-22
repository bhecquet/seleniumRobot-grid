package com.infotel.seleniumrobot.grid;

import java.net.URL;
import java.time.Duration;

import org.openqa.grid.internal.DefaultGridRegistry;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.remote.http.HttpClient;

import com.seleniumtests.util.NetworkUtility;

public class CustomGridRegistry extends DefaultGridRegistry {

	// The following needs to be volatile because we expose a public setters
	protected volatile Hub hub;

	public CustomGridRegistry() {
		this(null);
	}

	public CustomGridRegistry(Hub hub) {
		super(hub);
	}

	/**
	 * Use a different timeout between proxy status calls and driver calls
	 * Internal calls should be quick to reply. For driver calls, read timeout is the same as for robot (6 minutes in mobile)
	 */
	@Override
	public HttpClient getHttpClient(URL url) {
		return getHttpClient(url, 400, 400);
	}
	
	@Override
	public HttpClient getHttpClient(URL url, int connectionTimeout, int readTimeout) {
		
		String className = Thread.currentThread().getStackTrace()[3].getClassName();
		String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
		
		// if caller is the RemoteProxy for status, use short read timeout
		if ("getProxyStatus".equals(methodName) && "org.openqa.grid.internal.BaseRemoteProxy".equals(className)) {
			return NetworkUtility.createClient(url, Duration.ofSeconds(15), Duration.ofSeconds(15));
			
		// new sessions should be quick to establish (beware of mobile testing)	
		} else if ("org.openqa.grid.internal.TestSession".equals(className) && "forwardNewSessionRequestAndUpdateRegistry".equals(Thread.currentThread().getStackTrace()[6].getMethodName())) {
			return NetworkUtility.createClient(url,Duration.ofSeconds(Math.min(90, readTimeout)), Duration.ofSeconds(Math.min(90, connectionTimeout)));
		
		// else, use a longer timeout
		} else {
			return NetworkUtility.createClient(url,Duration.ofSeconds(Math.min(120, readTimeout)), Duration.ofSeconds(Math.min(720, connectionTimeout)));
		}
	}
	
}
