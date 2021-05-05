package com.infotel.seleniumrobot.grid.tests.tasks;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.exceptions.TaskException;
import com.infotel.seleniumrobot.grid.tasks.CommandTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.util.osutility.OSCommand;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;

@PrepareForTest({OSCommand.class, System.class, SystemUtils.class, OSUtilityFactory.class})
public class TestCommandTask extends BaseMockitoTest {
	

	@Mock
	OSUtility osUtility;
	
	@BeforeMethod(groups={"grid"})
	public void setup() {
		PowerMockito.mockStatic(OSCommand.class);
		PowerMockito.mockStatic(System.class);
		PowerMockito.mockStatic(OSUtilityFactory.class);
		new LaunchConfig(new String[] {"-role", "node"});

		PowerMockito.when(OSCommand.executeCommandAndWait(ArgumentMatchers.any(String[].class), eq(30), isNull())).thenReturn("hello guys");
		PowerMockito.when(OSCommand.executeCommandAndWait(ArgumentMatchers.any(String[].class), eq(10), isNull())).thenReturn("hello guys 10");
		PowerMockito.when(OSUtilityFactory.getInstance()).thenReturn(osUtility);
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandLinux() throws IOException {
		
		List<String> args = new ArrayList<>();
		args.add("hello");
		CommandTask cmdTask = CommandTask.getInstance();
		cmdTask.setCommand("echo", args, null);
		cmdTask.execute();
		
		Assert.assertEquals(cmdTask.getResult(), "hello guys");
		
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Linux");

		// check script has been launched
		PowerMockito.verifyStatic(OSCommand.class);
		OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 30, null);
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandLinuxWithTimeout() throws IOException {
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("echo", args, 10);
		cmdTask.execute();
		
		Assert.assertEquals(cmdTask.getResult(), "hello guys 10");
		
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Linux");
		
		// check script has been launched
		PowerMockito.verifyStatic(OSCommand.class);
		OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 10, null);
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandWindows() throws IOException {
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("echo", args);
		cmdTask.execute();
		
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows");
		
		// check script has been launched
		PowerMockito.verifyStatic(OSCommand.class);
		OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 30, null);
	}
	
	@Test(groups={"grid"})
	public void testExecuteCommandMac() throws IOException {
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("echo", args);
		cmdTask.execute();
		
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Mac");
		
		// check script has been launched
		PowerMockito.verifyStatic(OSCommand.class);
		OSCommand.executeCommandAndWait(new String[] {"echo", "hello"}, 30, null);
	}
	
	/**
	 * Test with an empty command. Command not called
	 * @throws IOException
	 */
	@Test(groups={"grid"}, expectedExceptions = TaskException.class)
	public void testExecuteCommandEmpty() throws IOException {

		PowerMockito.when(System.getProperty("os.name")).thenReturn("Mac");
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
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Mac");
		
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
		PowerMockito.when(System.getProperty("os.name")).thenReturn("Mac");
		
		CommandTask cmdTask = new CommandTask();
		List<String> args = new ArrayList<>();
		args.add("hello");
		cmdTask.setCommand("foo", args);
		cmdTask.execute();
	}
	
}
