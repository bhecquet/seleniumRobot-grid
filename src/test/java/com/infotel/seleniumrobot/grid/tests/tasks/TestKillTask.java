package com.infotel.seleniumrobot.grid.tests.tasks;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;

public class TestKillTask extends BaseMockitoTest {


	@Mock
	OSUtility osUtility;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		new LaunchConfig(new String[] {"node"});
	}
	
	@Test(groups= {"grid"})
	public void testKillWithPid() throws Exception {
		try (MockedStatic mockedOsUtilityFactory = mockStatic(OSUtilityFactory.class)) {
			mockedOsUtilityFactory.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);
			new KillTask().withPid(1000L).execute();
			verify(osUtility).killProcess("1000", true);
		}
	}
	
	@Test(groups= {"grid"})
	public void testKillWithName() throws Exception {
		try (MockedStatic mockedOsUtilityFactory = mockStatic(OSUtilityFactory.class)) {
			mockedOsUtilityFactory.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);
			new KillTask().withName("foo").execute();
			verify(osUtility).killProcessByName("foo", true);
		}
	}
	
	@Test(groups= {"grid"})
	public void testKillNoPidNoName() throws Exception {
		try (MockedStatic mockedOsUtilityFactory = mockStatic(OSUtilityFactory.class)) {
			mockedOsUtilityFactory.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);
			new KillTask().execute();
			verify(osUtility, never()).killProcessByName(anyString(), anyBoolean());
			verify(osUtility, never()).killProcess(anyString(), anyBoolean());
		}
	}
}
