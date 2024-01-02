package com.infotel.seleniumrobot.grid.tests.tasks;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.infotel.seleniumrobot.grid.tasks.CommandTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;

public class TestCommandTask extends BaseMockitoTest {
	

	@Mock
	OSUtility osUtility;

	private MockedStatic mockedOsCommand;
	private MockedStatic mockedOSUtilityFactory;
	private MockedStatic mockedOSUtility;

	@BeforeMethod(groups={"grid"})
	public void setup() {
		mockedOsCommand = mockStatic(OSCommand.class);
		mockedOSUtilityFactory = mockStatic(OSUtilityFactory.class);
		mockedOSUtility = mockStatic(OSUtility.class, CALLS_REAL_METHODS);

		new LaunchConfig(new String[] {"node"});

		mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(ArgumentMatchers.any(String[].class), eq(30), isNull())).thenReturn("hello guys");
		mockedOsCommand.when(() -> OSCommand.executeCommandAndWait(ArgumentMatchers.any(String[].class), eq(10), isNull())).thenReturn("hello guys 10");
		mockedOSUtilityFactory.when(() -> OSUtilityFactory.getInstance()).thenReturn(osUtility);
	}

	@AfterMethod(groups={"grid"}, alwaysRun = true)
	private void closeMocks() {
		mockedOsCommand.close();
		mockedOSUtilityFactory.close();
		mockedOSUtility.close();
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandLinux() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
		mockedOSUtility.when(() -> OSUtility.isLinux()).thenReturn(true);

		List<String> args = new ArrayList<>();
		args.add("hello");
		CommandTask cmdTask = CommandTask.getInstance();
		cmdTask.setCommand("echo", args, null);
		cmdTask.execute();
		
		Assert.assertEquals(cmdTask.getResult(), "hello guys");

		// check script has been launched
		mockedOsCommand.verify(() -> OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 30, null));
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandLinuxWithTimeout() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
		mockedOSUtility.when(() -> OSUtility.isLinux()).thenReturn(true);
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("echo", args, 10);
		cmdTask.execute();
		
		Assert.assertEquals(cmdTask.getResult(), "hello guys 10");
		
		// check script has been launched
		mockedOsCommand.verify(() -> OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 10, null));
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandWindows() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(true);
		mockedOSUtility.when(() -> OSUtility.isLinux()).thenReturn(false);
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("echo", args);
		cmdTask.execute();

		
		// check script has been launched
		mockedOsCommand.verify(() -> OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 30, null));
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandMac() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
		mockedOSUtility.when(() -> OSUtility.isMac()).thenReturn(true);
		mockedOSUtility.when(() -> OSUtility.isLinux()).thenReturn(false);
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("echo", args);
		cmdTask.execute();

		// check script has been launched
		mockedOsCommand.verify(() -> OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 30, null));
	}
	

	@Test(groups={"grid"})
	public void testExecuteCommandInPath() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(true);
		mockedOSUtility.when(() -> OSUtility.isLinux()).thenReturn(false);
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand(OSCommand.USE_PATH + "echo", args);
		cmdTask.execute();

		// check script has been launched with the "USE_PATH" pattern so that OSCommand class knows it needs to search un path
		mockedOsCommand.verify(() -> OSCommand.executeCommandAndWait(new String[] {"_USE_PATH_echo", "hello"}, 30, null));
	}
	
	/**
	 * Test with an empty command. Command not called
	 * @throws IOException
	 */
	@Test(groups={"grid"}, expectedExceptions = TaskException.class)
	public void testExecuteCommandEmpty() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
		mockedOSUtility.when(() -> OSUtility.isMac()).thenReturn(true);

		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("", args);
		cmdTask.execute();
	}
	
	/**
	 * Test with an null command. Command not called
	 * @throws IOException
	 */
	@Test(groups={"grid"}, expectedExceptions = TaskException.class)
	public void testExecuteCommandNull() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
		mockedOSUtility.when(() -> OSUtility.isMac()).thenReturn(true);
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand(null, args);
		cmdTask.execute();
	}
	
	/**
	 * Test with a command not allowed. Should not be executed
	 * @throws IOException
	 */
	@Test(groups={"grid"}, expectedExceptions = TaskException.class)
	public void testExecuteCommandNotAllowed() throws IOException {
		mockedOSUtility.when(() -> OSUtility.isWindows()).thenReturn(false);
		mockedOSUtility.when(() -> OSUtility.isMac()).thenReturn(true);
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("foo", args);
		cmdTask.execute();
	}
	
}
