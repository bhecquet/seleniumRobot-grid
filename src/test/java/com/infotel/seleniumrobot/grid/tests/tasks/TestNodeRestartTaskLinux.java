package com.infotel.seleniumrobot.grid.tests.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.mockito.ArgumentMatchers;
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
public class TestNodeRestartTaskLinux extends BaseMockitoTest {
	

	@Mock
	OSUtility osUtility;
	
	private String upgradeDir = Paths.get(Utils.getRootdir(), "upgrade", "node_upgrade_1").toString();
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		PowerMockito.mockStatic(OSCommand.class);
		PowerMockito.mockStatic(System.class);
		PowerMockito.mockStatic(OSUtilityFactory.class);
		new LaunchConfig(new String[] {"-role", "node"});

		PowerMockito.when(OSCommand.executeCommand(ArgumentMatchers.anyString())).thenReturn(null);
		PowerMockito.when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
	}
	
	@Test(groups={"grid"})
	public void testExecuteWithLinux() throws IOException {
		
		// prepare files
		FileUtils.write(Paths.get(upgradeDir, "seleniumRobot-grid-full.jar").toFile(), "", Charset.forName("UTF-8"));
		
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Linux");
		
		new NodeRestartTask().execute(0);
		
		// check script has been launched
		PowerMockito.verifyStatic(OSCommand.class);
		OSCommand.executeCommand("sh " + Utils.getRootdir() + File.separator + "launch.sh -role node");
		
		// check launch script content
		String content = FileUtils.readFileToString(Paths.get(Utils.getRootdir(), "launch.sh").toFile(), Charset.forName("UTF-8"));
		Assert.assertTrue(content.contains("if [ -f upgrade/seleniumRobot-grid-full.jar ]\n" +
"then\n" +
"		echo updating grid\n" +
"		cp -f upgrade/seleniumRobot-grid-full.jar seleniumRobot-grid-full.jar\n" +
"		rm -Rf upgrade\n" +
"fi\n" +
"java  -cp *:. com.infotel.seleniumrobot.grid.GridStarter \"$@\""));
	}
	
	@Test(groups={"grid"}, expectedExceptions=TaskException.class)
	public void testExecuteUpgradeFolderUnavailable() throws IOException {
		// prepare files, folder structure not conform
		FileUtils.write(Paths.get(Utils.getRootdir(), "upgrade", "seleniumRobot-grid.jar").toFile(), "", Charset.forName("UTF-8"));
		
		new NodeRestartTask().execute(0);
	}

	@Test(groups={"grid"}, expectedExceptions=TaskException.class)
	public void testExecuteUpgradeFileUnavailable() throws IOException {
		// prepare files, upgrade file has not the right name
		FileUtils.write(Paths.get(upgradeDir, "seleniumRobot-grid.jar").toFile(), "", Charset.forName("UTF-8"));
		
		new NodeRestartTask().execute(0);
	}
	
	
	/**
	 * delete all created files and folders
	 * @throws IOException 
	 */
	@AfterMethod(groups={"grid"})
	public void clean() throws IOException {
		FileUtils.deleteDirectory(new File(upgradeDir));
		Paths.get(Utils.getRootdir(), "launch.sh").toFile().delete();
	}
}
