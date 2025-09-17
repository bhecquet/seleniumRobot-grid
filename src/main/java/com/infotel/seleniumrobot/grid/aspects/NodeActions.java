package com.infotel.seleniumrobot.grid.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.grid.data.Availability;
import org.openqa.selenium.grid.data.NodeStatus;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.utils.GridStatus;

@Aspect
public class NodeActions {


	@Around("execution(public * org.openqa.selenium.grid.node.local.LocalNode.getStatus (..)) ")
	public Object onGetStatus(ProceedingJoinPoint joinPoint) throws Throwable {
		NodeStatus nodeStatus = (NodeStatus) joinPoint.proceed(joinPoint.getArgs());

		// in case node is set INACTIVE, send back a status that says to Grid not to send sessions anymore
		if (LaunchConfig.getCurrentNodeConfig().getStatus() == GridStatus.INACTIVE) {
			nodeStatus = new NodeStatus(nodeStatus.getNodeId(),
					nodeStatus.getExternalUri(),
					nodeStatus.getMaxSessionCount(),
					nodeStatus.getSlots(),
					Availability.DRAINING,
					nodeStatus.getHeartbeatPeriod(),
					nodeStatus.getSessionTimeout(),
					nodeStatus.getVersion(),
					nodeStatus.getOsInfo());
		}

		return nodeStatus;


	}
}
