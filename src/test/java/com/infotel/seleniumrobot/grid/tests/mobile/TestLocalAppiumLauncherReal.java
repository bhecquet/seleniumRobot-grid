package com.infotel.seleniumrobot.grid.tests.mobile;

import com.infotel.seleniumrobot.grid.mobile.LocalAppiumLauncher;
import com.seleniumtests.customexception.ConfigurationException;
import org.testng.SkipException;
import org.testng.annotations.Test;


public class TestLocalAppiumLauncherReal {

    @Test(groups={"it"})
    public void testAppiumStartup() {
        try {
            LocalAppiumLauncher appium = new LocalAppiumLauncher("D:\\tmp\\appium");
            appium.startAppium();
            appium.stopAppium();
        } catch (ConfigurationException e) {
            throw new SkipException("Test skipped, appium not correctly configured", e);
        }
    }
}
