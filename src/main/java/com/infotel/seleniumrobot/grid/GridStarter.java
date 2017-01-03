package com.infotel.seleniumrobot.grid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.common.CommandLineOptionHelper;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.selenium.GridLauncher;
import org.openqa.selenium.Platform;

import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.osutility.OSUtilityFactory;

public class GridStarter {
	
	private static final Logger logger = Logger.getLogger(GridStarter.class.getName());

	private String[] args;

    public GridStarter(String[] args) {
        this.args = args;
    }

    public String[] getArgs() {
		return args;
	}

	public static void main(String[] args) throws Exception {

        GridStarter starter = new GridStarter(args);
        starter.configure();
        starter.start();
    }
    
    /**
     * rewrite grid arguments to reflect the new configuration
     * @param newConfFile
     */
    public void rewriteArgs(File newConfFile) {
    	List<String> newArgs = new ArrayList<>();
		int nodeConfigIdx = -1;
		int i = 0;
		for (String arg: args) {
			if (nodeConfigIdx > -1) {
				newArgs.add(newConfFile.toString().replace(File.separator, "/"));
				nodeConfigIdx = -1;
				continue;
			}
			if ("-nodeConfig".equals(arg)) {
				nodeConfigIdx = i;
			} 
			newArgs.add(arg);
			
			i++;
		}
		args = newArgs.toArray(new String[0]);
    }
    
    /**
     * Method for adding driver path to json configuration
     */
    private void rewriteJsonConf() {
    	CommandLineOptionHelper helper = new CommandLineOptionHelper(args);
    	if (helper.isParamPresent("-nodeConfig")) {
    		String value = helper.getParamValue("-nodeConfig");
    		try {
    			String ext = OSUtilityFactory.getInstance().getProgramExtension();
    			String driverPath = Utils.getDriverDir().toString().replace(File.separator, "/");
    			String platformName = Platform.getCurrent().family().toString().toLowerCase();
    			
				JSONObject gridConf = new JSONObject(FileUtils.readFileToString(new File(value)));
				JSONObject configNode = gridConf.getJSONObject("configuration");
				configNode.put(String.format("Dwebdriver.chrome.driver=%s/chromedriver%s", driverPath, ext), "");
				configNode.put(String.format("Dwebdriver.gecko.driver=%s/geckodriver%s", driverPath, ext), "");
				
				if ("windows".equals(platformName)) {
					configNode.put(String.format("Dwebdriver.edge.driver=%s/MicrosoftWebDriver%s", driverPath, ext), "");
					configNode.put(String.format("Dwebdriver.ie.driver=%s/IEDriverServer%s", driverPath, ext), "");
				}
				
				File newConfFile = Paths.get(new File(value).getAbsoluteFile().getParent(), "rewritten_" + new File(value).getName()).toFile();
				FileUtils.writeStringToFile(newConfFile, gridConf.toString(4));
				
				// rewrite args with new configuration
				rewriteArgs(newConfFile);
				
			} catch (JSONException | IOException e) {
				throw new GridException("Cannot read file " + value, e);
			}
    	}
    }
    
    private void killExistingDrivers() {
    	OSUtilityFactory.getInstance().killAllWebDriverProcess();
    }
    
    private void extractDriverFiles() throws IOException {
    	
    	Path driverPath = Utils.getDriverDir();
    	driverPath.toFile().mkdirs();
    	
    	// get list of all drivers for this platform
    	String platformName = Platform.getCurrent().family().toString().toLowerCase();
    	InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(String.format("drivers/%s", platformName));
    	BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
    	String driverName;
    	while ((driverName = rdr.readLine()) != null) {
    		InputStream driver = Thread.currentThread().getContextClassLoader().getResourceAsStream(String.format("drivers/%s/%s", platformName, driverName));
    		try {
    			Files.copy(driver, Paths.get(driverPath.toString(), driverName), StandardCopyOption.REPLACE_EXISTING);
    			logger.info(String.format("Driver %s copied to %s", driverName, driverPath));
    		} catch (IOException e) {
    			logger.info(String.format("Driver not copied: %s - it may be in use", driverName));
    		}
        }
    	
    	// in case of Edge driver, copy the driver corresponding to windows version
    	if ("windows 10".equalsIgnoreCase(System.getProperty("os.name"))) {
    		String driverVersion = OSUtilityFactory.getInstance().getOSBuild().split("\\.")[2];
    		FileUtils.copyFile(Paths.get(driverPath.toString(), "MicrosoftWebDriver_" + driverVersion + ".exe").toFile(), 
    						   Paths.get(driverPath.toString(), "MicrosoftWebDriver.exe").toFile());
    	}
    	
    	// for IE copy the right version
    	if (OSUtilityFactory.getInstance().getIEVersion() < 10) {
    		FileUtils.copyFile(Paths.get(driverPath.toString(), "IEDriverServer_x64.exe").toFile(), 
					   			Paths.get(driverPath.toString(), "IEDriverServer.exe").toFile());
    	} else {
    		FileUtils.copyFile(Paths.get(driverPath.toString(), "IEDriverServer_Win32.exe").toFile(), 
		   						Paths.get(driverPath.toString(), "IEDriverServer.exe").toFile());
    	}
    }

    private void configure() throws IOException {
    	killExistingDrivers();
    	extractDriverFiles();
    	rewriteJsonConf();
    }

    private void start() throws Exception {
        GridLauncher.main(args);
    }
}
