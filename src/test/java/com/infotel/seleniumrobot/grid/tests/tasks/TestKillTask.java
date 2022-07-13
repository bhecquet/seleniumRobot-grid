package com.infotel.seleniumrobot.grid.tests.tasks;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.KillTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;

@PrepareForTest({OSUtilityFactory.class})
public class TestKillTask extends BaseMockitoTest {


	@Mock
	OSUtility osUtility;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		PowerMockito.mockStatic(OSUtilityFactory.class);
		new LaunchConfig(new String[] {"node"});

		PowerMockito.when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
	}
	
	@Test(groups= {"grid"})
	public void testKillWithPid() throws Exception {
		new KillTask().withPid(1000L).execute();
		verify(osUtility).killProcess("1000", true);
	}
	
	@Test(groups= {"grid"})
	public void testKillWithName() throws Exception {
		new KillTask().withName("foo").execute();
		verify(osUtility).killProcessByName("foo", true);
	}
	
	@Test(groups= {"grid"})
	public void testKillNoPidNoName() throws Exception {
		new KillTask().execute();
		verify(osUtility, never()).killProcessByName(anyString(), anyBoolean());
		verify(osUtility, never()).killProcess(anyString(), anyBoolean());
	}
}
