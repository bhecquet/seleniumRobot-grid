package com.infotel.seleniumrobot.grid.tests;

import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

@PowerMockIgnore({"javax.net.ssl.*", "com.google.inject.*", "javax.imageio.*", "javax.management.*",
	"javax.xml.*", "com.sun.org.apache.xerces.*", "org.xml.*", // to avoid: java.lang.IllegalAccessError: class javax.xml.parsers.FactoryFinder
	"org.w3c.dom.*", // to avoid error java.lang.LinkageError: loader constraint violation: loader org.powermock.core.classloader.javassist.JavassistMockClassLoader @1e16c0aa (instance of org.powermock.core.classloader.javassist.JavassistMockClassLoader, child of 'app' jdk.internal.loader.ClassLoaders$AppClassLoader) wants to load interface org.w3c.dom.Document. A different interface with the same name was previously loaded by 'bootstrap'.
	})
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
