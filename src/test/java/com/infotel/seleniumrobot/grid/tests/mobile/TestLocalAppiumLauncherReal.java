package com.infotel.seleniumrobot.grid.tests.mobile;

import com.infotel.seleniumrobot.grid.mobile.LocalAppiumLauncher;
import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.util.osutility.OSCommand;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    @Test
    public void test() throws IOException {
        String line = "cmd /C dir";
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.execute(cmdLine);
        System.out.println("-------------");
        String output = new OSCommand("where node", 5, StandardCharsets.UTF_8).execute();
        System.out.println(output);
        System.out.println("-------------");
    }
}
