package com.infotel.seleniumrobot.grid;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.RemoteException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.listeners.CommandListener;
import org.openqa.grid.internal.listeners.SelfHealingProxy;
import org.openqa.grid.internal.listeners.TestSessionListener;
import org.openqa.grid.internal.listeners.TimeoutListener;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.grid.internal.utils.HtmlRenderer;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.selenium.remote.http.HttpClient;

import com.seleniumtests.util.NetworkUtility;

public class CustomRemoteProxyWrapper implements RemoteProxy,
TimeoutListener,
SelfHealingProxy,
CommandListener,
TestSessionListener {
	
	private CustomRemoteProxy wrappedProxy;
	private boolean isMobileSlot = false;

	public CustomRemoteProxyWrapper(CustomRemoteProxy wrappedProxy) {
		this.wrappedProxy = wrappedProxy; 
	}

	@Override
	public void beforeCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
		wrappedProxy.beforeCommand(session, request, response);
	}

	@Override
	public void afterCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
		wrappedProxy.afterCommand(session, request, response);
	}

	@Override
	public void beforeSession(TestSession session) {
		wrappedProxy.beforeSession(session);
	}

	@Override
	public void afterSession(TestSession session) {
		wrappedProxy.afterSession(session);
	}

	@Override
	public void beforeRelease(TestSession session) {
		wrappedProxy.beforeRelease(session);
	}

	public boolean isAlive() {
		return wrappedProxy.isAlive();
	}

	@Override
	public boolean hasCapability(Map<String, Object> requestedCapability) {
		return wrappedProxy.hasCapability(requestedCapability);
	}
	
	/**
	 * Returns the HTTP client with timeouts that depend on type of slot.
	 * For mobile slots, timeout are greater because simulators can take time to create. In Mobile, seleniumRobot uses a 6 mins timeout instead of the 2 mins one for desktop
	 */
	public HttpClient getHttpClient(URL url, int connectionTimeout, int readTimeout) {
		int newSessionTimeout = 90;
		int internalTimeout = 15;
		int otherTimeout = 120;
		
		if (isMobileSlot) {
			newSessionTimeout = 300;
			otherTimeout = 350;
		} 
		
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
		
		// if caller is the RemoteProxy for status, use short read timeout
		if ("getProxyStatus".equals(methodName) && "org.openqa.grid.internal.BaseRemoteProxy".equals(className)) {
			return NetworkUtility.createClient(url, Duration.ofSeconds(internalTimeout), Duration.ofSeconds(internalTimeout));
			
		// new sessions should be quick to establish (beware of mobile testing)	
		} else if ("org.openqa.grid.internal.TestSession".equals(className) && "forwardNewSessionRequestAndUpdateRegistry".equals(Thread.currentThread().getStackTrace()[5].getMethodName())) {
			return NetworkUtility.createClient(url,Duration.ofSeconds(Math.min(newSessionTimeout, readTimeout)), Duration.ofSeconds(Math.min(newSessionTimeout, connectionTimeout)));
		
		// else, use a longer timeout
		} else {
			return NetworkUtility.createClient(url,Duration.ofSeconds(Math.min(otherTimeout, readTimeout)), Duration.ofSeconds(Math.min(otherTimeout, connectionTimeout)));
		}
	}

	public void setMobileSlot(boolean isMobileSlot) {
		this.isMobileSlot = isMobileSlot;
	}

	@Override
	public int compareTo(RemoteProxy arg0) {
		return wrappedProxy.compareTo(arg0);
	}

	@Override
	public List<TestSlot> getTestSlots() {
		return wrappedProxy.getTestSlots();
	}

	@Override
	public <T extends GridRegistry> T getRegistry() {
		return wrappedProxy.getRegistry();
	}

	@Override
	public CapabilityMatcher getCapabilityHelper() {
		return wrappedProxy.getCapabilityHelper();
	}

	@Override
	public void setupTimeoutListener() {
		wrappedProxy.setupTimeoutListener();
		
	}

	@Override
	public String getId() {
		return wrappedProxy.getId();
	}

	@Override
	public void teardown() {
		wrappedProxy.teardown();
		
	}

	@Override
	public GridNodeConfiguration getConfig() {
		return wrappedProxy.getConfig();
	}

	@Override
	public RegistrationRequest getOriginalRegistrationRequest() {
		return wrappedProxy.getOriginalRegistrationRequest();
	}

	@Override
	public int getMaxNumberOfConcurrentTestSessions() {
		return wrappedProxy.getMaxNumberOfConcurrentTestSessions();
	}

	@Override
	public URL getRemoteHost() {
		return wrappedProxy.getRemoteHost();
	}

	@Override
	public TestSession getNewSession(Map<String, Object> requestedCapability) {
		return wrappedProxy.getNewSession(requestedCapability);
	}

	@Override
	public int getTotalUsed() {
		return wrappedProxy.getTotalUsed();
	}

	@Override
	public HtmlRenderer getHtmlRender() {
		return wrappedProxy.getHtmlRender();
	}

	@Override
	public int getTimeOut() {
		return wrappedProxy.getTimeOut();
	}

	@Override
	public HttpClient getHttpClient(URL url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getProxyStatus() {
		return wrappedProxy.getProxyStatus();
	}

	@Override
	public boolean isBusy() {
		return wrappedProxy.isBusy();
	}

	@Override
	public float getResourceUsageInPercent() {
		return wrappedProxy.getResourceUsageInPercent();
	}

	@Override
	public long getLastSessionStart() {
		return wrappedProxy.getLastSessionStart();
	}

	@Override
	public void startPolling() {
		wrappedProxy.startPolling();
		
	}

	@Override
	public void stopPolling() {
		wrappedProxy.stopPolling();
		
	}

	@Override
	public void addNewEvent(RemoteException event) {
		wrappedProxy.addNewEvent(event);
		
	}

	@Override
	public void onEvent(List<RemoteException> events, RemoteException lastInserted) {
		wrappedProxy.onEvent(events, lastInserted);
		
	} 
}
