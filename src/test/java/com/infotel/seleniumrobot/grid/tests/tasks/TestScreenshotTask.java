package com.infotel.seleniumrobot.grid.tests.tasks;

import java.awt.GraphicsEnvironment;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.infotel.seleniumrobot.grid.tasks.ScreenshotTask;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;

public class TestScreenshotTask extends BaseMockitoTest {

	@Test(groups= {"grid"})
	public void testScreenshot() {
		ScreenshotTask task = new ScreenshotTask().execute();
		if (GraphicsEnvironment.isHeadless()) {
			Assert.assertNull(task.getScreenshot());
		} else {
			Assert.assertTrue(task.getWidth() > 0);
			Assert.assertTrue(task.getHeight() > 0);
			Assert.assertNotNull(task.getScreenshot());
		}
	}
}
