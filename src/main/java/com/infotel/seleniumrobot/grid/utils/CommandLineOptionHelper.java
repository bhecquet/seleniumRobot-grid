/*
Copyright 2011 Selenium committers
Copyright 2011 Software Freedom Conservancy
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Code copied from Selenium 2.x
*/
package com.infotel.seleniumrobot.grid.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.grid.common.exception.GridConfigurationException;

public class CommandLineOptionHelper {

	private List<String> args;

	@SuppressWarnings("unused")
	private CommandLineOptionHelper() {

	}

	public CommandLineOptionHelper(List<String> args) {
		this.args = args;
	}

	public boolean isParamPresent(String name) {
		for (String arg : args) {
			if (name.equalsIgnoreCase(arg)) {
				return true;
			}
		}
		return false;
	}

	public String getParamValue(String name) {
		int index = -1;
		for (int i = 0; i < args.size(); i++) {
			if (name.equals(args.get(i))) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			throw new GridConfigurationException("The parameter " + name + " isn't specified.");
		}
		if (args.size() == index) {
			throw new GridConfigurationException("The parameter " + name + " doesn't have a value specified.");
		}

		if (((index + 1) < args.size()) && !args.get(index + 1).startsWith("-")) {
			return args.get(index + 1);
		} else {
			return "";
		}
	}

	public List<String> getParamValues(String name) {
		if (isParamPresent(name)) {
			String value = getParamValue(name);
			return Arrays.asList(value.split(","));
		} else {
			return new ArrayList<>();
		}

	}

	/**
	 * get all occurrences of -name
	 * 
	 * @param name
	 * @return A List of Strings that have the passed name argument in them.
	 */
	public List<String> getAll(String name) {
		List<String> res = new ArrayList<>();
		for (int i = 0; i < args.size(); i++) {
			if (name.equals(args.get(i))) {
				res.add(args.get(i + 1));
			}
		}
		return res;
	}
	
	public List<String> getAll() {
		return new ArrayList<>(args);
	}
	
	/**
	 * Remove all parameters with the given name
	 * @param name
	 * @return
	 */
	public List<String> removeAll(String name) {
		List<String> res = new ArrayList<>();
		for (int i = 0; i < args.size(); i++) {
			if (name.equals(args.get(i))) {
				i++;
			} else {
				res.add(args.get(i));
				
				try {
					res.add(args.get(i + 1));
					i++;
				} catch (IndexOutOfBoundsException e) {}
			}
		}
		return res;
	}

	public List<String> getKeys() {
		List<String> keys = new ArrayList<>();
		for (String arg : args) {
			if (arg.startsWith("-")) {
				keys.add(arg);
			}
		}
		return keys;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

}
