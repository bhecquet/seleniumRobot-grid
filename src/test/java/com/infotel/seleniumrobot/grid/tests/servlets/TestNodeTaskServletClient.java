package com.infotel.seleniumrobot.grid.tests.servlets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

@PrepareForTest({Unirest.class})
public class TestNodeTaskServletClient extends BaseMockitoTest {
	
	@Mock
	GetRequest getRequest;
	
	@Mock
	HttpResponse<String> response;
	
	private void prepareMock(String reply) throws UnirestException {
		PowerMockito.mockStatic(Unirest.class);

		when(Unirest.get("http://localhost:4567/extra/NodeTaskServlet/")).thenReturn(getRequest);
		when(getRequest.queryString(anyString(), anyString())).thenReturn(getRequest);
		when(getRequest.asString()).thenReturn(response);
		when(response.getBody()).thenReturn(reply);
	}
	
	@Test(groups={"grid"})
	public void testGetDriverPids() throws UnirestException {
		
		NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
		prepareMock("100,200");
		
		Assert.assertEquals(client.getDriverPids("chrome", "67.0", new ArrayList<>()), Arrays.asList(100L, 200L));
	}
	
	@Test(groups={"grid"})
	public void testGetDriverPidsEmptyList() throws UnirestException {
		
		NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
		prepareMock("");
		
		Assert.assertEquals(client.getDriverPids("chrome", "67.0", new ArrayList<>()), new ArrayList<>() );
	}
	
	@Test(groups={"grid"})
	public void testGetBrowserAndDriverPids() throws UnirestException {
		
		NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
		prepareMock("101,201");
		
		Assert.assertEquals(client.getBrowserAndDriverPids("chrome", "67.0", new ArrayList<>()), Arrays.asList(101L, 201L));
	}
	
	@Test(groups={"grid"})
	public void testGetBrowserAndDriverPidsEmptyList() throws UnirestException {
		
		NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
		prepareMock("");
		
		Assert.assertEquals(client.getBrowserAndDriverPids("chrome", "67.0", new ArrayList<>()), new ArrayList<>() );
	}
}
