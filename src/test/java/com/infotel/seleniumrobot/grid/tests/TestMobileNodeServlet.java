package com.infotel.seleniumrobot.grid.tests;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.MobileNodeServlet;
import com.seleniumtests.browserfactory.mobile.MobileDeviceSelector;
import com.seleniumtests.customexception.ConfigurationException;


public class TestMobileNodeServlet extends BaseServletTest {

    private Server mobileInfoServer;
    private HttpHost serverHost;
    
    @Mock
    MobileDeviceSelector mobileDeviceSelector;
    
    @InjectMocks
    MobileNodeServlet nodeServlet = new MobileNodeServlet();

    @BeforeMethod
    public void setUp() throws Exception {
        mobileInfoServer = startServerForServlet(nodeServlet, "/" + MobileNodeServlet.class.getSimpleName() + "/*");
        serverHost = new HttpHost("localhost", ((ServerConnector)mobileInfoServer.getConnectors()[0]).getLocalPort());
    }

    @AfterMethod
    public void tearDown() throws Exception {
    	mobileInfoServer.stop();
    }

    @Test(groups={"grid"})
    public void getShouldReturnUpdatedCaps() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();

    	DesiredCapabilities caps = new DesiredCapabilities();
    	caps.setCapability("platformName", "android");
    	caps.setCapability("platformVersion", "5.0");
    	caps.setCapability("deviceName", "Nexus 5");
    	DesiredCapabilities updatedCaps = new DesiredCapabilities();
    	updatedCaps.setCapability("platformName", "android");
    	updatedCaps.setCapability("platformVersion", "5.0");
    	updatedCaps.setCapability("deviceName", "145687");
    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(caps)).thenReturn(updatedCaps);
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	for (Entry<String, ?> entry: caps.asMap().entrySet()) {
    		builder.setParameter(entry.getKey(), entry.getValue().toString());
    	}
    	
    	HttpGet httpGet = new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	JSONObject reply = new JSONObject(IOUtils.toString(execute.getEntity().getContent()));
    	
    	Assert.assertEquals(reply.getString("deviceName"), "145687");
   
    }
    
    @Test(groups={"grid"})
    public void getShouldThrowError() throws IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();

    	when(mobileDeviceSelector.updateCapabilitiesWithSelectedDevice(Mockito.any(DesiredCapabilities.class))).thenThrow(new ConfigurationException("device not found"));
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/MobileNodeServlet/");
    	
    	HttpGet httpGet = new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 404);
    	
    }

}
