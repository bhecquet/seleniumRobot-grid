package com.infotel.seleniumrobot.grid.tests;

import javax.servlet.http.HttpServlet;

import org.mockito.MockitoAnnotations;
import org.powermock.modules.testng.PowerMockTestCase;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.servlet.ServletContextHandler;
import org.seleniumhq.jetty9.servlet.ServletHolder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;


public abstract class BaseServletTest extends PowerMockTestCase {

    protected Server startServerForServlet(HttpServlet servlet, String path) throws Exception {
        Server server = new Server(0);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(servlet), path);
        server.start();

        return server;
    }
    
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
