package com.infotel.seleniumrobot.grid.mobile;

import com.seleniumtests.customexception.ConfigurationException;
import com.seleniumtests.customexception.ScenarioException;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.logging.SeleniumRobotLogger;
import com.seleniumtests.util.osutility.*;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.semver4j.Semver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalAppiumLauncher {

    private String appiumVersion;
    private String appiumHome;
    private String nodeVersion;
    private String nodeCommand;
    private String nodeCommandDetach;
    private Process appiumProcess;
    private ProcessInfo appiumNodeProcess;
    private long appiumPort;
    private String logFile = null;
    private String optionString = "";
    private static Object appiumLauncherLock = new Object();

    private static Logger logger = SeleniumRobotLogger.getLogger(LocalAppiumLauncher.class);

    public LocalAppiumLauncher() {
        this(null);
    }

    public LocalAppiumLauncher(String logDirectory) {

        appiumPort = 4723 + Math.round(Math.random() * 1000);
        if (logDirectory != null) {
            new File(logDirectory).mkdirs();
            if (new File(logDirectory).isDirectory()) {
                logFile = Paths.get(logDirectory, String.format("appium-%d.log", appiumPort)).toString();
            }
        }

        checkInstallation();
        generateOptions();

    }

    public Process getAppiumProcess() {
        return appiumProcess;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    public long getAppiumPort() {
        return appiumPort;
    }

    public void setAppiumPort(long appiumPort) {
        this.appiumPort = appiumPort;
    }

    /**
     * Method for generating options passed to appium (e.g: logging)
     */
    private void generateOptions() {
        if (logFile != null) {
            optionString += String.format(" --relaxed-security --log %s --log-level debug:debug", logFile);
        }
    }

    private void checkAppiumVersion() {
        File packageFile = Paths.get(appiumHome, "node_modules", "appium", "package.json").toFile();
        try {
            String appiumConfig = FileUtils.readFileToString(packageFile, StandardCharsets.UTF_8);
            JSONObject packages = new JSONObject(appiumConfig);
            if (!"appium".equals(packages.getString("name"))) {
                throw new ConfigurationException(String.format("package.json file found in %s is not for appium, check path", packageFile.getAbsolutePath()));
            }

            appiumVersion = packages.getString("version");

        } catch (IOException e) {
            throw new ConfigurationException(String.format("File %s not found, appium does not seem to be installed in %s", packageFile, appiumHome), e);
        }
    }

    /**
     * Check that node and appium are installed
     * Appium is expected to be found in <APPIUM_PATH>/node_modules/appium/
     */
    private void checkInstallation() {
        appiumHome = SystemUtility.getenv("APPIUM_PATH");
        if (appiumHome != null) {
            if (Paths.get(appiumHome, "node").toFile().exists()
                    || Paths.get(appiumHome, "node.exe").toFile().exists()) {
                nodeCommand = Paths.get(appiumHome, "node").toString();
            } else {
                if (OSUtility.isWindows()) {
                    nodeCommand = new OSCommand(List.of("node")).searchInWindowsPath("node");
                } else {
                    nodeCommand = "node";
                }
            }
        } else {
            throw new ConfigurationException("APPIUM_PATH environment variable not set");
        }

        // get appium version
        checkAppiumVersion();

        // get version for node
        String reply = OSCommand.executeCommandAndWait(new String[]{nodeCommand, "-v"}).trim();
        if (!reply.matches("v\\d++\\.\\d++.*")) {
            throw new ConfigurationException("Node does not seem to be installed, is environment variable APPIUM_PATH set ?");
        } else {
            nodeVersion = reply;
        }

        if (OSUtility.isWindows()) {
            nodeCommandDetach = "cmd /c start /MIN cmd /C \"" + nodeCommand + "\"";
        } else {
            nodeCommandDetach = nodeCommand;
        }
    }

    /**
     * Call /wd/hub/sessions to see if appium is started
     */
    private void waitAppiumAlive() {

        String endPoint = appiumVersion.startsWith("3") ? "appium/sessions" : "sessions";
        for (int i = 0; i < 60; i++) {
            try {
                HttpResponse<String> response = Unirest.get(getAppiumServerUrl() + endPoint).asString();

                if (response.getStatus() == 200) {
                    logger.info("appium has started");
                    break;
                }
            } catch (UnirestException e) {
                logger.info("appium not started");
            }
            WaitHelper.waitForSeconds(1);
        }
    }

    /**
     * Returns the local appium URL
     *
     * @return
     */
    public String getAppiumServerUrl() {
        if (appiumVersion.startsWith("1")) {
            return String.format("http://localhost:%d/wd/hub/", appiumPort);
        } else {
            return String.format("http://localhost:%d/", appiumPort);
        }
    }

    /**
     * Start appium and wait for availability
     * To work around windows launching, which spawns a new cmd (we cannot stop the underlying node process),
     * get the node process PID associated to the newly created appium
     */
    public void startAppiumWithWait() {

        synchronized (appiumLauncherLock) {

            List<ProcessInfo> nodeProcessesInitial = OSUtilityFactory.getInstance().getRunningProcesses("node");

            startAppiumWithoutWait();

            // wait for startup
            waitAppiumAlive();

            for (ProcessInfo nodeProcess : OSUtilityFactory.getInstance().getRunningProcesses("node")) {
                if (!nodeProcessesInitial.contains(nodeProcess)) {
                    appiumNodeProcess = nodeProcess;
                    break;
                }
            }
        }
    }

    public void startAppiumWithoutWait() {

        // correction for "socket hang up" error when starting test
        // TODO: this fix does not handle multiple tests in parallel, but for now, only one mobile test can be done on mac on one session
        if (OSUtility.isMac()) {
            OSCommand.executeCommand("killall iproxy xcodebuild XCTRunner");
        }

        Semver appiumVers = new Semver(appiumVersion);
        appiumProcess = OSCommand.executeCommand(String.format("%s %s/node_modules/appium/index.js --port %d %s",
                nodeCommandDetach,
                appiumHome,
                appiumPort,
                optionString));

    }

    /**
     * Start appium process
     */

    public void startAppium() {
        startAppiumWithWait();
    }

    /**
     * Stops appium process if it has been started, else, raise a ScenarioException
     */

    public void stopAppium() {
        if (appiumProcess == null) {
            throw new ScenarioException("Appium process has never been started");
        }
        appiumProcess.destroy();

        if (appiumNodeProcess != null) {
            OSUtilityFactory.getInstance().killProcess(appiumNodeProcess.getPid(), true);
        }

    }

    public String getAppiumVersion() {
        return appiumVersion;
    }

    public List<String> getDriverList() {
        List<String> driverList = new ArrayList<>();
        String output = OSCommand.executeCommandAndWait(String.format("%s %s/node_modules/appium/index.js driver list",
                nodeCommand,
                appiumHome));

        Pattern pattern = Pattern.compile("- (.+?) \\[(.*)\\]");
        for (String line : output.split("\\n")) {
            line = line.replaceAll("\u001B\\[[;\\d]*m", "");
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches() && !matcher.group(2).contains("not installed")) {
                driverList.add(matcher.group(1).split("@")[0]);
            }

        }

        return driverList;
    }

}
