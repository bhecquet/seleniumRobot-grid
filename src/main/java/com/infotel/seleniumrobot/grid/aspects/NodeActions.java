package com.infotel.seleniumrobot.grid.aspects;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.RetrySessionRequestException;
import org.openqa.selenium.grid.data.Availability;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.data.NodeStatus;
import org.openqa.selenium.grid.node.local.LocalNode;
import org.openqa.selenium.internal.Either;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.utils.GridStatus;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;

@Aspect
public class NodeActions {


	@Around("execution(public * org.openqa.selenium.grid.node.local.LocalNode.getStatus (..)) ")
	public Object onGetStatus(ProceedingJoinPoint joinPoint) throws Throwable {
		System.out.println("get status aspect");
		NodeStatus nodeStatus = (NodeStatus) joinPoint.proceed(joinPoint.getArgs());

		// in case node is set INACTIVE, send back a status that says to Grid not to send sessions anymore
		if (LaunchConfig.getCurrentNodeConfig().getStatus() == GridStatus.INACTIVE) {
			nodeStatus = new NodeStatus(nodeStatus.getNodeId(),
					nodeStatus.getExternalUri(),
					nodeStatus.getMaxSessionCount(),
					nodeStatus.getSlots(),
					Availability.DRAINING,
					nodeStatus.getHeartbeatPeriod(),
					nodeStatus.getVersion(),
					nodeStatus.getOsInfo());
		}

		return nodeStatus;


	}
}
