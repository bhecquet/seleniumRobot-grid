package com.infotel.seleniumrobot.grid.tests.tasks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.tasks.CleanNodeTask;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

@PrepareForTest({Advapi32Util.class, OSUtilityFactory.class, CleanNodeTask.class, FileUtils.class, OSUtility.class})
public class TestCleanNodeTask extends BaseMockitoTest {


	@Mock
	OSUtility osUtility;
	
	@BeforeMethod(groups={"grid"})
	public void setup() throws Exception {
		PowerMockito.mockStatic(Advapi32Util.class);
		PowerMockito.mockStatic(OSUtilityFactory.class);
		PowerMockito.spy(FileUtils.class);
		PowerMockito.spy(OSUtility.class);
		
		PowerMockito.when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
	}
	
	/**
	 * Check drivers / browser are not cleaned
	 * @throws Exception 
	 */
	@Test(groups={"grid"})
	public void testDevMode() throws Exception {

		Path videoFile = Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER, "video.avi");
		
		try {
			Files.delete(videoFile);
		} catch (IOException e) {}
		

		FileUtils.write(videoFile.toFile(), "foo", StandardCharsets.UTF_8);
		WaitHelper.waitForSeconds(1);
		new LaunchConfig(new String[] {"node", "--devMode", "true"});
		CleanNodeTask task = new CleanNodeTask();
		task.execute();
		
		// no file deleted (too young)
		Assert.assertTrue(videoFile.toFile().exists());
		
		// browser not cleaned (dev mode)
		verify(osUtility, never()).killAllWebBrowserProcess(anyBoolean());
		verify(osUtility, never()).killAllWebDriverProcess();
		verify(osUtility, never()).killProcessByName(anyString(), anyBoolean());
		PowerMockito.verifyStatic(FileUtils.class, never());
		FileUtils.cleanDirectory(any(File.class));
		
		PowerMockito.verifyStatic(Advapi32Util.class, never());
		Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "AutoDetect", 1);
	}
	
	@Test(groups={"grid"})
	public void testRunMode() throws Exception {
		
		Path videoFile = Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER, "video.avi");
		
		try {
			Files.delete(videoFile);
		} catch (IOException e) {}
		

		FileUtils.write(videoFile.toFile(), "foo", StandardCharsets.UTF_8);
		WaitHelper.waitForSeconds(1);
		new LaunchConfig(new String[] {"node", "--devMode", "false"});
		CleanNodeTask task = new CleanNodeTask();
		task.execute();
		
		// no file deleted (too young)
		Assert.assertTrue(videoFile.toFile().exists());
		
		// browser not cleaned (dev mode)
		verify(osUtility).killAllWebBrowserProcess(anyBoolean());
		verify(osUtility).killAllWebDriverProcess();
		verify(osUtility).killProcessByName(anyString(), anyBoolean());
		PowerMockito.verifyStatic(FileUtils.class);
		FileUtils.cleanDirectory(any(File.class));
		
		PowerMockito.verifyStatic(Advapi32Util.class, never());
		Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "AutoDetect", 1);
		
	}
	
	@Test(groups={"grid"})
	public void testRunModeCleanVideo() throws Exception {
		
		Path videoFile = Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER, "video.avi");
		
		try {
			Files.delete(videoFile);
		} catch (IOException e) {}
		
		
		FileUtils.write(videoFile.toFile(), "foo", StandardCharsets.UTF_8);
		WaitHelper.waitForSeconds(1);
		new LaunchConfig(new String[] {"node", "--devMode", "false"});
		CleanNodeTask task = new CleanNodeTask(0);
		task.execute();
		
		// no file deleted (too young)
		Assert.assertFalse(videoFile.toFile().exists());

		PowerMockito.verifyStatic(Advapi32Util.class, never());
		Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "AutoDetect", 1);
		
	}
	
	/**
	 * Check auto-proxy config is restored on windows if requested
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testRunModeRestoreAutoProxy() throws Exception {
		
		PowerMockito.when(OSUtility.class, "isWindows").thenReturn(true);
		
		Path videoFile = Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER, "video.avi");
		
		try {
			Files.delete(videoFile);
		} catch (IOException e) {}
		
		
		FileUtils.write(videoFile.toFile(), "foo", StandardCharsets.UTF_8);
		WaitHelper.waitForSeconds(1);
		new LaunchConfig(new String[] {"node", "--devMode", "false", "--proxyConfig", "auto"});
		CleanNodeTask task = new CleanNodeTask(0);
		task.execute();
		
		// no file deleted (too young)
		Assert.assertFalse(videoFile.toFile().exists());
		
		PowerMockito.verifyStatic(Advapi32Util.class);
		Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "AutoDetect", 1);
		
	}
	
	/**
	 * Check auto-proxy config is not restored on linux if requested
	 * @throws Exception
	 */
	@Test(groups={"grid"})
	public void testRunModeRestoreAutoProxyNotWindows() throws Exception {
		
		PowerMockito.when(OSUtility.class, "isWindows").thenReturn(false);
		
		Path videoFile = Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER, "video.avi");
		
		try {
			Files.delete(videoFile);
		} catch (IOException e) {}
		
		
		FileUtils.write(videoFile.toFile(), "foo", StandardCharsets.UTF_8);
		WaitHelper.waitForSeconds(1);
		new LaunchConfig(new String[] {"node", "--devMode", "false", "--proxyConfig", "auto"});
		CleanNodeTask task = new CleanNodeTask(0);
		task.execute();
		
		// no file deleted (too young)
		Assert.assertFalse(videoFile.toFile().exists());
		
		PowerMockito.verifyStatic(Advapi32Util.class, never());
		Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "AutoDetect", 1);
		
	}
}
