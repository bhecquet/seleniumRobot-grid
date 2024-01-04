package com.infotel.seleniumrobot.grid.tests.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.DiscoverBrowserAndDriverPidsTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.osutility.OSUtility;

import static org.mockito.Mockito.*;

public class TestDiscoverAndDriverPidsTask extends BaseMockitoTest {

	private BrowserInfo firefoxInfo;
	private BrowserInfo firefoxInfo2;
	private BrowserInfo firefoxInfo3;

	private MockedStatic mockedOsUtility;

	@BeforeMethod(groups={"grid"})
	public void setup() {
		firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "90.0", "/usr/bin/firefox", false, true));
		firefoxInfo2 = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "91.0", "/usr/bin/firefox2", false, true));
		firefoxInfo3 = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "92.0", "/usr/bin/firefox3", false, true));
		mockedOsUtility = mockStatic(OSUtility.class, CALLS_REAL_METHODS);
		new LaunchConfig(new String[] {"node"});
		
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo));

		mockedOsUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browserInfos);

	}

	@AfterMethod(groups = "grid", alwaysRun = true)
	private void closeMocks() {
		mockedOsUtility.close();
	}
	
	@Test(groups= {"grid"})
	public void testExecuteWithExistingPid() throws Exception {
		doReturn(Arrays.asList(2000L)).when(firefoxInfo).getDriverAndBrowserPid(Arrays.asList(1000L));
		
		DiscoverBrowserAndDriverPidsTask task = new DiscoverBrowserAndDriverPidsTask("firefox", "90.0")
			.withExistingPids(Arrays.asList(1000L))
			.execute();
		Assert.assertEquals(task.getProcessPids(), Arrays.asList(2000L));
	}
	
	/**
	 * Check the right browser is choosen
	 * @throws Exception
	 */
	@Test(groups= {"grid"})
	public void testExecuteWithMultipleBrowsers() throws Exception {
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo, firefoxInfo2, firefoxInfo3));

		mockedOsUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browserInfos);

		doReturn(Arrays.asList(2000L)).when(firefoxInfo).getDriverAndBrowserPid(Arrays.asList(1000L));
		doReturn(Arrays.asList(3000L)).when(firefoxInfo2).getDriverAndBrowserPid(Arrays.asList(1000L));
		doReturn(Arrays.asList(4000L)).when(firefoxInfo3).getDriverAndBrowserPid(Arrays.asList(1000L));
		
		DiscoverBrowserAndDriverPidsTask task = new DiscoverBrowserAndDriverPidsTask("firefox", "91.0")
				.withExistingPids(Arrays.asList(1000L))
				.execute();
		Assert.assertEquals(task.getProcessPids(), Arrays.asList(3000L)); // pid from the second firefox browser
	}
	
	/**
	 * Check one broser is choosen even if version does not match (it takes the last one)
	 * @throws Exception
	 */
	@Test(groups= {"grid"})
	public void testExecuteWithMultipleBrowsersWrongVersion() throws Exception {
		Map<BrowserType, List<BrowserInfo>> browserInfos = new HashMap<>();
		browserInfos.put(BrowserType.FIREFOX, Arrays.asList(firefoxInfo, firefoxInfo2, firefoxInfo3));

		mockedOsUtility.when(() -> OSUtility.getInstalledBrowsersWithVersion()).thenReturn(browserInfos);
		
		when(firefoxInfo.getDriverAndBrowserPid(Arrays.asList(1000L))).thenReturn(Arrays.asList(2000L));
		when(firefoxInfo2.getDriverAndBrowserPid(Arrays.asList(1000L))).thenReturn(Arrays.asList(3000L));
		when(firefoxInfo3.getDriverAndBrowserPid(Arrays.asList(1000L))).thenReturn(Arrays.asList(4000L));
		
		DiscoverBrowserAndDriverPidsTask task = new DiscoverBrowserAndDriverPidsTask("firefox", "98.0")
				.withExistingPids(Arrays.asList(1000L))
				.execute();
		Assert.assertEquals(task.getProcessPids(), Arrays.asList(4000L)); // pid from the second firefox browser, as we get at least one browser
	}
	
	/**
	 * Requested driver has not been discovered (should never happen)
	 * @throws Exception
	 */
	@Test(groups= {"grid"})
	public void testExecuteWithExistingPidNoBrowserInfo() throws Exception {
		doReturn(Arrays.asList(2000L)).when(firefoxInfo).getDriverAndBrowserPid(Arrays.asList(1000L));
		
		DiscoverBrowserAndDriverPidsTask task = new DiscoverBrowserAndDriverPidsTask("chrome", "90.0")
				.withExistingPids(Arrays.asList(1000L))
				.execute();
		Assert.assertEquals(task.getProcessPids(), new ArrayList<>());
	}
	
	@Test(groups= {"grid"})
	public void testExecuteWithExistingPidNull() throws Exception {
		doReturn(Arrays.asList(2000L)).when(firefoxInfo).getDriverAndBrowserPid(Arrays.asList(1000L));
		
		DiscoverBrowserAndDriverPidsTask task = new DiscoverBrowserAndDriverPidsTask("firefox", "90.0")
				.execute();
		Assert.assertEquals(task.getProcessPids(), new ArrayList<>());
	}

	@Test(groups= {"grid"})
	public void testExecuteWithParentPid() throws Exception {
		doReturn(Arrays.asList(2000L)).when(firefoxInfo).getAllBrowserSubprocessPids(Arrays.asList(1000L));
		
		DiscoverBrowserAndDriverPidsTask task = new DiscoverBrowserAndDriverPidsTask("firefox", "90.0")
			.withParentsPids(Arrays.asList(1000L))
			.execute();
		Assert.assertEquals(task.getProcessPids(), Arrays.asList(2000L));
	}
}
