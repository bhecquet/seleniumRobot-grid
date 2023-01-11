package com.infotel.seleniumrobot.grid.aspects;

import java.io.File;
import java.util.Map.Entry;

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

import io.appium.java_client.remote.AndroidMobileCapabilityType;

@Aspect
public class RelaySessionFactoryActions {
	
	private static final MobileDeviceSelector mobileDeviceSelector;
	private static final String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/") + "/";
	private static final String ext = OSUtilityFactory.getInstance().getProgramExtension();
	
	static {
		mobileDeviceSelector = new MobileDeviceSelector();
		mobileDeviceSelector.initialize();
	}
	
	
	@Around("execution(public * org.openqa.selenium.grid.node.relay.RelaySessionFactory.test (..)) ")
	public Object onTest(ProceedingJoinPoint joinPoint) throws Throwable {
		System.out.println("test");
		
		return joinPoint.proceed(joinPoint.getArgs());
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
			if (platformName != null && platformName.is(Platform.IOS) || platformName.is(Platform.ANDROID)) {
				
				updatedCapabilities = mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(new MutableCapabilities(capabilities), DriverMode.LOCAL);
				
				if (updatedCapabilities.getCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE) != null) {
					updatedCapabilities.setCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE, 
							driverPath + updatedCapabilities.getCapability(SeleniumRobotCapabilityType.APPIUM_PREFIX + AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE) + ext);
				}

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

			return joinPoint.proceed(new Object[] {new CreateSessionRequest(sessionRequest.getDownstreamDialects(), updatedCapabilities, sessionRequest.getMetadata())});
		} catch (ConfigurationException e) {
			return joinPoint.proceed(new Object[] { sessionRequest });
		}
	
		
	} 
}