package com.infotel.seleniumrobot.grid.aspects;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.remote.http.HttpRequest;

@Aspect
public class TestSessionForward {

	
	@AfterReturning(pointcut="execution(private * org.openqa.grid.internal.TestSession.prepareProxyRequest (..)) ", returning="retVal")
	public void reprepareRequest(Object retVal) {
		System.out.println("coucou");
		HttpRequest proxyRequest = (HttpRequest)retVal;
		
		try {
			URI url = new URI(proxyRequest.getUri());
			proxyRequest.removeHeader("Host");
			proxyRequest.addHeader("Host", String.format("%s:%d", url.getHost(), url.getPort()));
		} catch (URISyntaxException e) {
			
		}
	}
}
