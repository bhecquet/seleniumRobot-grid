package com.infotel.seleniumrobot.grid.tests.aspects;

import com.infotel.seleniumrobot.grid.aspects.NodeActions;
import com.infotel.seleniumrobot.grid.aspects.RelaySessionFactoryActions;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.aspectj.lang.ProceedingJoinPoint;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.node.relay.RelaySessionFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class TestRelaySessionFactoryActions extends BaseMockitoTest {

    @Mock
    ProceedingJoinPoint joinPoint;

    @Mock
    RelaySessionFactory relaySessionFactory;

    RelaySessionFactoryActions relaySessionFactoryActions;

    @BeforeMethod(groups={"grid"}, alwaysRun = true)
    public void setup() throws Exception {
        relaySessionFactoryActions = spy(new RelaySessionFactoryActions());
    }

    @Test(groups={"grid"})
    public void testOnTestNoNodeTagsRequested() throws Throwable {
        Capabilities requestedCaps = new UiAutomator2Options().setPlatformVersion("14");
        when(joinPoint.getArgs()).thenReturn(new Object[] {requestedCaps});
        when(joinPoint.proceed(any())).thenReturn(true);

        relaySessionFactoryActions.onTest(joinPoint);

        ArgumentCaptor<Object[]> caps = ArgumentCaptor.forClass(Object[].class);
        verify(joinPoint).proceed(caps.capture());
        Capabilities checkedCaps = (Capabilities) caps.getValue()[0];
        Assert.assertEquals(requestedCaps.asMap(), checkedCaps.asMap());
    }

    @Test(groups={"grid"})
    public void testOnTestNodeTagsRequestedNoTagOnSlot() throws Throwable {
        MutableCapabilities requestedCaps = new UiAutomator2Options().setPlatformVersion("14");
        requestedCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, List.of("foo"));
        when(joinPoint.getArgs()).thenReturn(new Object[] {requestedCaps});
        when(joinPoint.proceed(any())).thenReturn(true);
        when(joinPoint.getThis()).thenReturn(relaySessionFactory);
        when(relaySessionFactory.getStereotype()).thenReturn(new MutableCapabilities());

        Boolean reply = (Boolean) relaySessionFactoryActions.onTest(joinPoint);
        Assert.assertFalse(reply);

        // proceed never called as nodeTags do not match
        verify(joinPoint, never()).proceed(any());
    }

    @Test(groups={"grid"})
    public void testOnTestNodeTagsRequestedWrongTagOnSlot() throws Throwable {
        MutableCapabilities requestedCaps = new UiAutomator2Options().setPlatformVersion("14");
        requestedCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, List.of("foo"));
        when(joinPoint.getArgs()).thenReturn(new Object[] {requestedCaps});
        when(joinPoint.proceed(any())).thenReturn(true);
        when(joinPoint.getThis()).thenReturn(relaySessionFactory);
        MutableCapabilities stereotype = new MutableCapabilities();
        stereotype.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, List.of("bar"));
        when(relaySessionFactory.getStereotype()).thenReturn(stereotype);

        Boolean reply = (Boolean) relaySessionFactoryActions.onTest(joinPoint);
        Assert.assertFalse(reply);

        // proceed never called as nodeTags do not match
        verify(joinPoint, never()).proceed(any());
    }

    @Test(groups={"grid"})
    public void testOnTestNodeTagsRequestedRightTagOnSlot() throws Throwable {
        MutableCapabilities requestedCaps = new UiAutomator2Options().setPlatformVersion("14");
        requestedCaps.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, List.of("foo"));
        when(joinPoint.getArgs()).thenReturn(new Object[] {requestedCaps});
        when(joinPoint.proceed(any())).thenReturn(true);
        when(joinPoint.getThis()).thenReturn(relaySessionFactory);
        MutableCapabilities stereotype = new MutableCapabilities();
        stereotype.setCapability(SeleniumRobotCapabilityType.NODE_TAGS, List.of("bar", "foo"));
        when(relaySessionFactory.getStereotype()).thenReturn(stereotype);

        Boolean reply = (Boolean) relaySessionFactoryActions.onTest(joinPoint);

        ArgumentCaptor<Object[]> caps = ArgumentCaptor.forClass(Object[].class);
        verify(joinPoint).proceed(caps.capture());
        Capabilities checkedCaps = (Capabilities) caps.getValue()[0];

        // check requested capabilities has been updated so that RelaySessionFactory matcher doesn't block on that capability
        Assert.assertEquals(checkedCaps.getCapability(SeleniumRobotCapabilityType.NODE_TAGS), List.of("bar", "foo"));
    }
}
