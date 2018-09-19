package com.infotel.seleniumrobot.grid.tests.utils;

import java.util.ArrayList;
import java.util.Arrays;

import org.openqa.grid.common.exception.GridConfigurationException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.utils.CommandLineOptionHelper;

public class TestCommandLineOptionHelper {

	@Test(groups={"grid"})
	public void testIsPresent() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node"}));
		Assert.assertTrue(cmd.isParamPresent("-role"));
	}
	
	@Test(groups={"grid"})
	public void testIsNotPresent() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node"}));
		Assert.assertFalse(cmd.isParamPresent("-rle"));
	}
	
	@Test(groups={"grid"})
	public void testGetParamValue() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node"}));
		Assert.assertEquals(cmd.getParamValue("-role"), "node");
	}
	
	@Test(groups={"grid"})
	public void testGetParamNoValue() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role"}));
		Assert.assertEquals(cmd.getParamValue("-role"), "");
	}
	
	@Test(groups={"grid"}, expectedExceptions=GridConfigurationException.class)
	public void testGetParamNotExist() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role"}));
		cmd.getParamValue("-rle");
	}
	
	@Test(groups={"grid"})
	public void testGetParamValues() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node,hub"}));
		Assert.assertEquals(cmd.getParamValues("-role"), Arrays.asList("node", "hub"));
	}
	
	@Test(groups={"grid"})
	public void testGetParamNoValues() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node,hub"}));
		Assert.assertEquals(cmd.getParamValues("-rle"), new ArrayList<>());
	}
	
	@Test(groups={"grid"})
	public void testGetAllParams() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node", "-role", "hub"}));
		Assert.assertEquals(cmd.getAll("-role"), Arrays.asList("node", "hub"));
	}
	
	@Test(groups={"grid"})
	public void testRemoveAllParams() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node", "-conf", "myConf", "-role", "hub"}));
		Assert.assertEquals(cmd.removeAll("-role"), Arrays.asList("-conf", "myConf"));
	}
	
	@Test(groups={"grid"})
	public void testGetKeys() {
		CommandLineOptionHelper cmd = new CommandLineOptionHelper(Arrays.asList(new String[] {"-role", "node", "-conf", "myConf"}));
		Assert.assertEquals(cmd.getKeys(), Arrays.asList("-role", "-conf"));
	}
}
