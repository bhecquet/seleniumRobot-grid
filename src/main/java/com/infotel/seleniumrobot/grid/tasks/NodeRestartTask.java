package com.infotel.seleniumrobot.grid.tasks;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtilityFactory;

/**
 * Task to restart a node
 * This will also check if an update is available
 * @author behe
 *
 */
public class NodeRestartTask {
	
	private static final Logger logger = Logger.getLogger(NodeRestartTask.class);
	private static final String JAR_FILE_NAME = "seleniumRobot-grid-full.jar";
	
	public void execute() {
		execute(5);
	}
	
	/**
	 * generates the restart file
	 * Kills the current process
	 */
	public void execute(int restartDelay) {
		
		// search for new version. It should be in a subdirectory of 'upgrade' dir. Move it to upgrade folder directly
		File upgradeDir = Paths.get(Utils.getRootdir(), "upgrade").toFile();
		try {
			
			File subDirUpgrade = upgradeDir.listFiles((FileFilter)DirectoryFileFilter.INSTANCE)[0];
			FileUtils.copyFileToDirectory(Paths.get(subDirUpgrade.getPath(), JAR_FILE_NAME).toFile(), upgradeDir);
		} catch (IOException | ArrayIndexOutOfBoundsException e) {
			throw new TaskException(String.format("no upgrade file found in %s", upgradeDir.toString()), e);
		}
		
		
		String generatedLauncher;
		try {
			generatedLauncher = generateLaunchScript();
		} catch (IOException e) {
			throw new TaskException("Cannot generate launch script", e);
		}
		
		// start the new version
		String command;
		if (SystemUtils.IS_OS_WINDOWS) {
			command = "cmd /C " + generatedLauncher;
		} else {
			command = "sh " + generatedLauncher;
		}
		
		command += " " + String.join(" ", LaunchConfig.getCurrentLaunchConfig().getOriginalArgs());
		logger.info("execute command: " + command);
		OSCommand.executeCommand(command);
		WaitHelper.waitForSeconds(restartDelay);
		logger.info("now killing");
		
		// kill current
		Long pid = Utils.getCurrentPID();
		OSUtilityFactory.getInstance().killProcess(pid.toString(), true);
	}
	
	private String generateLaunchScript() throws IOException {
		String launchFilePath = Utils.getRootdir() + File.separator;
		String content;
		
		if (SystemUtils.IS_OS_WINDOWS) {
			launchFilePath += "launch.bat";
			content = String.format("CD /d \"%%~dp0\"\r\n"
					+ "if exist upgrade\\%s (\r\n"
					+ "		rem upgrade grid\r\n"
					+ "		xcopy /s /y \"upgrade\\%s\" \"%s\"\r\n"
					+ "		rd /s /q upgrade\r\n"
					+ ")\r\n"
					+ "java  -cp *;. com.infotel.seleniumrobot.grid.GridStarter %%*\r\n" 
					+ "EXIT /B %%ERRORLEVEL%%", JAR_FILE_NAME, JAR_FILE_NAME, JAR_FILE_NAME);
		} else {
			launchFilePath += "launch.sh";
			content = String.format("if [ -f upgrade/%s ]\n"
					+ "then\n"
					+ "		echo updating grid\n"
					+ "		cp -f upgrade/%s %s\n"
					+ "		rm -Rf upgrade\n"
					+ "fi\n"
					+ "java  -cp *:. com.infotel.seleniumrobot.grid.GridStarter \"$@\"\n", JAR_FILE_NAME, JAR_FILE_NAME, JAR_FILE_NAME);
		}
		
		FileUtils.write(new File(launchFilePath), content);
		
		return launchFilePath;
		
	}
	
	
}
