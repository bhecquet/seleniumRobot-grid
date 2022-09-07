package com.infotel.seleniumrobot.grid.aspects;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.RetrySessionRequestException;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.node.local.LocalNode;
import org.openqa.selenium.internal.Either;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

@Aspect
public class NodeActions {

	
//	@Around("execution(public * org.openqa.selenium.grid.node.local.LocalNode..* (..)) ")
//	public Object logLocalNode(ProceedingJoinPoint joinPoint) throws Throwable {
//		System.out.println("coucou: " + joinPoint.getSignature());
//		Object reply = joinPoint.proceed(joinPoint.getArgs());
//		
//		return reply;
//	} 
	
	@Around("execution(public * org.openqa.selenium.grid.node.local.LocalNode.newSession (..)) ")
	public Object onNewSession(ProceedingJoinPoint joinPoint) throws Throwable {
		CreateSessionRequest sessionRequest = (CreateSessionRequest) joinPoint.getArgs()[0];
		
		LocalNode node = (LocalNode) joinPoint.getThis();
		
		if (node.getCurrentSessionCount() >= LaunchConfig.getCurrentLaunchConfig().getMaxSessions() 
				&& (sessionRequest.getDesiredCapabilities().getCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE) == null 
				|| !sessionRequest.getDesiredCapabilities().getCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE).toString().equals(node.getExternalUri().toString()))
				) {

			return Either.left(new RetrySessionRequestException("Max session count reached."));
	    }

		return joinPoint.proceed(joinPoint.getArgs());
	} 
	
	@Around("execution(public * org.openqa.selenium.grid.node.local.LocalNode.getStatus (..)) ")
	public Object onGetStatus(ProceedingJoinPoint joinPoint) throws Throwable {
		keepAlive();
		return joinPoint.proceed(joinPoint.getArgs());
	} 
	
	@Around("execution(public * org.openqa.selenium.grid.node.local.LocalNode.isSupporting (..)) ")
	public Object onIsSupporting(ProceedingJoinPoint joinPoint) throws Throwable {
		// in case node is marked as INACTIVE, we reply that it's not supporting any capabilities so that no new session are affected
		if (LaunchConfig.getCurrentNodeConfig().getStatus() == GridStatus.INACTIVE) {
			return false;
		} else {
			return joinPoint.proceed(joinPoint.getArgs());
		}
	} 
	
	public void keepAlive() {

		// do not clear drivers and browser when devMode is true
		if (!LaunchConfig.getCurrentLaunchConfig().getDevMode()) {
			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			if (mouseLocation != null) {
				double choice = Math.random();
				try {
					if (choice > 0.5) {
						new Robot().mouseMove(mouseLocation.x - 1, mouseLocation.y);
					} else {
						new Robot().mouseMove(mouseLocation.x + 1, mouseLocation.y);
					}
				} catch (AWTException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
}
