package com.infotel.seleniumrobot.grid.tests.utils;

import org.eclipse.jetty.server.Server;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.utils.Utils;

public class TestUtils {

	@Test(groups={"grid"})
	public void portAlreadyBound() throws Exception {
		Server server = null;
		try {
			server = new Server(6666);
			server.start();
			Assert.assertTrue(Utils.portAlreadyInUse(6666));
		} finally {
			if (server != null && server.isStarted()) {
				server.stop();
			}
		}
	}
	
	@Test(groups={"grid"})
	public void portNotAlreadyBound() throws Exception {
		Assert.assertFalse(Utils.portAlreadyInUse(7777));
	}
	
	@Test(groups={"grid"})
	public void dynamicPortDefined() throws Exception {
		Assert.assertFalse(Utils.portAlreadyInUse(-1));
	}
}
