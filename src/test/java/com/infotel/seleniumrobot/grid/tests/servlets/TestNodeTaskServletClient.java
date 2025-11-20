package com.infotel.seleniumrobot.grid.tests.servlets;

import com.infotel.seleniumrobot.grid.servlets.client.NodeTaskServletClient;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import kong.unirest.core.GetRequest;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class TestNodeTaskServletClient extends BaseMockitoTest {

    @Mock
    GetRequest getRequest;

    @Mock
    HttpResponse<String> response;

    private MockedStatic mockedUnirest = null;

    private void prepareMock(String reply) throws UnirestException {
        mockedUnirest = mockStatic(Unirest.class);

        mockedUnirest.when(() -> Unirest.get("http://localhost:4577/extra/NodeTaskServlet")).thenReturn(getRequest);
        when(getRequest.queryString(anyString(), anyString())).thenReturn(getRequest);
        when(getRequest.asString()).thenReturn(response);
        when(response.getBody()).thenReturn(reply);
    }

    @AfterMethod(groups = "grid", alwaysRun = true)
    private void closeMocks() {
        if (mockedUnirest != null) {
            mockedUnirest.close();
        }
    }

    @Test(groups = {"grid"})
    public void testGetDriverPids() throws UnirestException, URISyntaxException {

        NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
        prepareMock("100,200");

        Assert.assertEquals(client.getDriverPids("chrome", "67.0", new ArrayList<>()), Arrays.asList(100L, 200L));
    }

    @Test(groups = {"grid"})
    public void testGetDriverPidsEmptyList() throws UnirestException, URISyntaxException {

        NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
        prepareMock("");

        Assert.assertEquals(client.getDriverPids("chrome", "67.0", new ArrayList<>()), new ArrayList<>());
    }

    @Test(groups = {"grid"})
    public void testGetBrowserAndDriverPids() throws UnirestException, URISyntaxException {

        NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
        prepareMock("101,201");

        Assert.assertEquals(client.getBrowserAndDriverPids("chrome", "67.0", new ArrayList<>()), Arrays.asList(101L, 201L));
    }

    @Test(groups = {"grid"})
    public void testGetBrowserAndDriverPidsEmptyList() throws UnirestException, URISyntaxException {

        NodeTaskServletClient client = new NodeTaskServletClient("localhost", 4567);
        prepareMock("");

        Assert.assertEquals(client.getBrowserAndDriverPids("chrome", "67.0", new ArrayList<>()), new ArrayList<>());
    }
}
