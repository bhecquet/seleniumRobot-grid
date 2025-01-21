package com.infotel.seleniumrobot.grid.aspects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.infotel.seleniumrobot.grid.mobile.LocalAppiumLauncher;
import com.seleniumtests.util.logging.SeleniumRobotLogger;
import io.appium.java_client.android.options.context.SupportsChromedriverExecutableOption;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.grid.data.CreateSessionRequest;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.server.FileServlet;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.browserfactory.mobile.MobileDeviceSelector;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.driver.DriverMode;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import org.openqa.selenium.grid.node.relay.RelaySessionFactory;


@Aspect
public class RelaySessionFactoryActions {
	
	private static final MobileDeviceSelector mobileDeviceSelector;
	private static final String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/") + "/";
	private static final String ext = OSUtilityFactory.getInstance().getProgramExtension();

	private static Logger logger = SeleniumRobotLogger.getLogger(RelaySessionFactoryActions.class);
	
	static {
		mobileDeviceSelector = new MobileDeviceSelector();
		mobileDeviceSelector.initialize();
	}

	@Around("execution(public * org.openqa.selenium.grid.node.relay.RelaySessionFactory.test (..)) ")
	public Object onTest(ProceedingJoinPoint joinPoint) throws Throwable {
		System.out.println("test");
		Capabilities requestedCapabilities = (Capabilities) joinPoint.getArgs()[0];

		// check nodeTags here, before giving hand to RelaySessionFactory
		// nodeTags may contain several values on slot, but only one in requested session
		// so, once we have checked that nodeTags matches, modify the requested capabilities to that RelaySessionFactory.test match on that capability
		if (requestedCapabilities.getCapability(SeleniumRobotCapabilityType.NODE_TAGS) != null) {
			try {
				List<String> requestedTags = (List<String>)requestedCapabilities.getCapability(SeleniumRobotCapabilityType.NODE_TAGS);
				List<String> slotTags = (List<String>)((RelaySessionFactory)(joinPoint.getThis())).getStereotype().getCapability(SeleniumRobotCapabilityType.NODE_TAGS);

				if (slotTags == null) {
					return false;
				}
				for (String tag: requestedTags) {
					if (!slotTags.contains(tag)) {
						return false;
					}
				}
			} catch (ClassCastException e) {
				logger.info("requested tags MUST be a list of strings, not a string");
			}

			Map<String, Object> tmpRequestedCapabilities = new HashMap<>(requestedCapabilities.asMap());
			tmpRequestedCapabilities.put(SeleniumRobotCapabilityType.NODE_TAGS, ((RelaySessionFactory)(joinPoint.getThis())).getStereotype().getCapability(SeleniumRobotCapabilityType.NODE_TAGS));
			requestedCapabilities = new MutableCapabilities(tmpRequestedCapabilities);
		}

		return joinPoint.proceed(new Object[] {requestedCapabilities});
	} 
	
	@Around("execution(public * org.openqa.selenium.grid.node.relay.RelaySessionFactory.apply (..)) ")
	public Object onApply(ProceedingJoinPoint joinPoint) throws Throwable {
		System.out.println("apply");
		CreateSessionRequest sessionRequest = (CreateSessionRequest) joinPoint.getArgs()[0];
		Capabilities capabilities = sessionRequest.getDesiredCapabilities();

		try {
			MutableCapabilities updatedCapabilities = new MutableCapabilities(capabilities);

			// update capabilities for mobile. Mobile tests are identified by the use of 'platformName' capability
			// this will allow to add missing caps, for example when client requests an android device without specifying it precisely
			Platform platformName = capabilities.getPlatformName();
			if (platformName != null && platformName.is(Platform.ANDROID)) {

				updatedCapabilities = mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(new MutableCapabilities(capabilities), DriverMode.LOCAL);

				if (updatedCapabilities.getCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + SupportsChromedriverExecutableOption.CHROMEDRIVER_EXECUTABLE_OPTION) != null) {
					updatedCapabilities.setCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + SupportsChromedriverExecutableOption.CHROMEDRIVER_EXECUTABLE_OPTION,
							driverPath + updatedCapabilities.getCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + SupportsChromedriverExecutableOption.CHROMEDRIVER_EXECUTABLE_OPTION) + ext);
				}
			}

			if (platformName != null && (platformName.is(Platform.IOS) || platformName.is(Platform.ANDROID))) {

				// replace all capabilities whose value begins with 'file:' by the remote HTTP URL
				// we assume that these files have been previously uploaded on hub and thus available
				for (Entry<String, Object> entry: capabilities.asMap().entrySet()) {
					if (entry.getValue() instanceof String && ((String)entry.getValue()).startsWith(FileServlet.FILE_PREFIX)) {
						updatedCapabilities.setCapability(entry.getKey(), String.format("http://%s:%d/grid/admin/FileServlet?file=%s", 
																 LaunchConfig.getCurrentNodeConfig().getNodeOptions().getPublicGridUri().get().getHost(), 
																		LaunchConfig.getCurrentNodeConfig().getNodeOptions().getPublicGridUri().get().getPort() + 10, // custom servlet is started on hub port + 10  
																		entry.getValue()));
					}
				}
			}

			if (platformName != null && platformName.is(Platform.WINDOWS)) {

				updatedCapabilities = new MutableCapabilities(capabilities.asMap()
						.entrySet()
						.stream()
						.filter(entry -> !entry.getKey().startsWith("sr:"))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
			}

			return joinPoint.proceed(new Object[] {new CreateSessionRequest(sessionRequest.getDownstreamDialects(), updatedCapabilities, sessionRequest.getMetadata())});
		} catch (ConfigurationException e) {
			logger.error("Error updating session", e);
			return joinPoint.proceed(new Object[] { sessionRequest });
		}
	
		
	} 
}
