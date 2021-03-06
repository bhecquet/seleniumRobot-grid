package com.infotel.seleniumrobot.grid.tests;

import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

@PowerMockIgnore({"javax.net.ssl.*", "com.google.inject.*", "javax.imageio.*"})
public class BaseMockitoTest extends PowerMockTestCase {

	@BeforeMethod(alwaysRun=true)  
	public void beforeMethod() throws Exception {
		beforePowerMockTestMethod();
		MockitoAnnotations.initMocks(this); 
	}
	
	@BeforeClass(alwaysRun=true)  
	public void beforeClass() throws Exception {
		beforePowerMockTestClass();
	}
	
	@AfterMethod(alwaysRun=true)
	public void afterMethod() throws Exception {
		afterPowerMockTestMethod();
	}
	
	@AfterClass(alwaysRun=true)
	public void afterClass() throws Exception {
		afterPowerMockTestClass();
	}
}
