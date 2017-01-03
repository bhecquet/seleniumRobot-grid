/**
 * Copyright 2017 www.infotel.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infotel.seleniumrobot.grid.tests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.GridStarter;

public class TestGridStarter {

	@Test(groups={"grid"})
	public void testArgsRewrite() throws IOException {
		File newConfFile = File.createTempFile("conf", ".json");
		GridStarter starter = new GridStarter(new String[] {"-role", "node", "-nodeConfig", "conf.json"});
		starter.rewriteArgs(newConfFile);
		Assert.assertTrue(Arrays.asList(starter.getArgs()).contains(newConfFile.toString().replace(File.separator, "/")));
		Assert.assertTrue(Arrays.asList(starter.getArgs()).contains("-nodeConfig"));
	}
	
	@Test(groups={"grid"})
	public void testArgsNoRewrite() throws IOException {
		File newConfFile = File.createTempFile("conf", ".json");
		GridStarter starter = new GridStarter(new String[] {"-role", "hub", "-hubConfig", "conf.json"});
		starter.rewriteArgs(newConfFile);
		Assert.assertFalse(Arrays.asList(starter.getArgs()).contains(newConfFile.toString().replace(File.separator, "/")));
		Assert.assertFalse(Arrays.asList(starter.getArgs()).contains("-nodeConfig"));
	}
}
