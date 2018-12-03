package com.infotel.seleniumrobot.grid;

import java.net.URL;
import java.time.Duration;

import org.openqa.grid.internal.DefaultGridRegistry;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.internal.OkHttpClient;

public class CustomGridRegistry extends DefaultGridRegistry {
	protected HttpClient.Factory customInternalHttpClientFactory;
	protected HttpClient.Factory customDriverHttpClientFactory;

	// The following needs to be volatile because we expose a public setters
	protected volatile Hub hub;

	public CustomGridRegistry() {
		this(null);
	}

	public CustomGridRegistry(Hub hub) {
		super(hub);
		customDriverHttpClientFactory = new OkHttpClient.Factory(Duration.ofMinutes(2), Duration.ofMinutes(6));
		customInternalHttpClientFactory = new OkHttpClient.Factory(Duration.ofSeconds(15), Duration.ofSeconds(15));
	}

	/**
	 * Use a different timeout between proxy status calls and driver calls
	 * Internal calls should be quick to reply. For driver calls, read timeout is the same as for robot (6 minutes in mobile)
	 */
	@Override
	public HttpClient getHttpClient(URL url) {
	
		String className = Thread.currentThread().getStackTrace()[3].getClassName();
		String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
		
		// if caller is the RemoteProxy for status, use short read timeout
		if ("getProxyStatus".equals(methodName) && "org.openqa.grid.internal.BaseRemoteProxy".equals(className)) {
			return customInternalHttpClientFactory.createClient(url);
			
		// else, use a longer timeout
		} else {
			return customDriverHttpClientFactory.createClient(url);
		}
	}
}
