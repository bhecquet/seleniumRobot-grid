package com.infotel.seleniumrobot.grid.tests.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.infotel.seleniumrobot.grid.tasks.NodeRestartTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;

@PrepareForTest({OSCommand.class, System.class, SystemUtils.class, OSUtilityFactory.class})
public class TestNodeRestartTaskWindows extends BaseMockitoTest {
	

	@Mock
	OSUtility osUtility;
	
	private String upgradeDir = Paths.get(Utils.getRootdir(), "upgrade", "node_upgrade_1").toString();
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		PowerMockito.mockStatic(OSCommand.class);
		PowerMockito.mockStatic(System.class);
		PowerMockito.mockStatic(OSUtilityFactory.class);
		new LaunchConfig(new String[] {"-role", "node"});

		PowerMockito.when(OSCommand.executeCommand(Matchers.anyString())).thenReturn(null);
		PowerMockito.when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
	}
	
	
	@Test(groups={"grid"})
	public void testExecuteWithWindows() throws IOException {
		// prepare files
		FileUtils.write(Paths.get(upgradeDir, "seleniumRobot-grid-full.jar").toFile(), "");
		
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows");
		
		new NodeRestartTask().execute(0);
		
		// check script has been launched
		PowerMockito.verifyStatic();
		OSCommand.executeCommand("cmd /C " + Utils.getRootdir() + File.separator + "launch.bat -role node");
		
		// check launch script content
		String content = FileUtils.readFileToString(Paths.get(Utils.getRootdir(), "launch.bat").toFile());
		Assert.assertTrue(content.contains("CD /d \"%~dp0\"\r\n" +
"if exist upgrade\\seleniumRobot-grid-full.jar (\r\n" +
"		rem upgrade grid\r\n" +
"		xcopy /s /y \"upgrade\\seleniumRobot-grid-full.jar\" \"seleniumRobot-grid-full.jar\"\r\n" +
"		rd /s /q upgrade\r\n" +
")\r\n" +
"java  -cp *;. com.infotel.seleniumrobot.grid.GridStarter %*\r\n" +
"EXIT /B %ERRORLEVEL%"));
	}
	
	
	/**
	 * delete all created files and folders
	 * @throws IOException 
	 */
	@AfterMethod(groups={"grid"})
	public void clean() throws IOException {
		FileUtils.deleteDirectory(new File(upgradeDir));
		Paths.get(Utils.getRootdir(), "launch.bat").toFile().delete();
	}
}
