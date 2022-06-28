package com.infotel.seleniumrobot.grid.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.servlets.server.NodeTaskServlet;
import com.infotel.seleniumrobot.grid.tasks.video.VideoCaptureTask;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.osutility.OSUtility;
import com.seleniumtests.util.osutility.OSUtilityFactory;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class CleanNodeTask implements Task {
	
	private static final Logger logger = LogManager.getLogger(CleanNodeTask.class);

	@SuppressWarnings("unchecked")
	@Override
	public CleanNodeTask execute() throws Exception {
		// delete video files older than 8 hours
		try {
			Files.walk(Paths.get(Utils.getRootdir(), VideoCaptureTask.VIDEOS_FOLDER))
			        .filter(Files::isRegularFile)
			        .filter(p -> p.toFile().lastModified() < Instant.now().minusSeconds(8 * 3600).toEpochMilli())
			        .forEach(t -> {
						try {
							Files.delete(t);
						} catch (IOException e) {}
					});
			
		} catch (IOException e) {
		}
		
		// do not clear drivers and browser when devMode is true
		if (LaunchConfig.getCurrentLaunchConfig().getDevMode()) {
			return this;
		}
		
		try {
			OSUtilityFactory.getInstance().killAllWebBrowserProcess(true);
			OSUtilityFactory.getInstance().killAllWebDriverProcess();
		} catch (Exception e) {
			logger.error("Error while kill browser / drivers", e);
		}
		
		File temp;
		try {
			temp = File.createTempFile("temp-file-name", ".tmp");
			File tempFolder = temp.getParentFile().getAbsoluteFile();
			FileUtils.cleanDirectory(tempFolder);
		} catch (IOException e) {
		} 	
		
		// kill popup raised on windows when a driver crashes on Windows
		if (OSUtility.isWindows()) {
			try {
				OSUtilityFactory.getInstance().killProcessByName("Werfault", true);
			} catch (Exception e) {
			}
		}
		
		// reset proxy setting to "automatic" if requested on windows
		if (LaunchConfig.getCurrentLaunchConfig().getProxyConfig() != null && LaunchConfig.getCurrentLaunchConfig().getProxyConfig().isAutodetect() && OSUtility.isWindows()) {
			setWindowsAutoDetectProxy();
		}
			
		return this;
	}
	
	/**
	 * Force proxy auto-detection
	 * 
	 * For information: https://stackoverflow.com/questions/1564627/how-to-set-automatic-configuration-script-for-a-dial-up-connection-programmati
	 * HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Internet Settings\Connections registry key has values for all connections that are defined in 'Internet Options' and for LAN settings too (DefaultConnectionSettings is for LAN). The values are byte arrays and here is the description of every byte:
		1) Byte number zero always has a 3C or 46 - I couldnt find more information about this byte.The next three bytes are zeros.
		2) Byte number 4 is a counter used by the 'Internet Options' property sheet (Internet explorer->Tools->Internet Options...). As you manually change the internet setting (such as LAN settings in the Connections tab), this counter increments.Its not very useful byte.But it MUST have a value.I keep it zero always.The next three bytes are zeros (Bytes 5 to 7).
		3) Byte number 8 can take different values as per your settings. The value is : 09 when only 'Automatically detect settings' is enabled 03 when only 'Use a proxy server for your LAN' is enabled 0B when both are enabled 05 when only 'Use automatic configuration script' is enabled 0D when 'Automatically detect settings' and 'Use automatic configuration script' are enabled 07 when 'Use a proxy server for your LAN' and 'Use automatic configuration script' are enabled 0F when all the three are enabled. 01 when none of them are enabled. The next three bytes are zeros (Bytes 9 to B).
		4) Byte number C (12 in decimal) contains the length of the proxy server address.For example a proxy server '127.0.0.1:80' has length 12 (length includes the dots and the colon).The next three bytes are zeros (Bytes D to F).
		5) Byte 10 (or 16 in decimal) contains the proxy server address - like '127.0.0.1:80' (where 80 is obviously the port number)
		6) the byte immediatley after the address contians the length of additional information.The next three bytes are zeros. For example if the 'Bypass proxy server for local addresses' is ticked, then this byte is 07,the next three bytes are zeros and then comes a string i.e. '' ( indicates that you are bypassing the proxy server.Now since has 7 characters, the length is 07!). You will have to experiment on your own for finding more about this. If you dont have any additional info then the length is 0 and no information is added.
		7) The byte immediately after the additional info, is the length of the automatic configuration script address (If you dont have a script address then you dont need to add anything,skip this step and goto step 8).The next three bytes are zeros,then comes the address.
		8) Finally, 32 zeros are appended.(I dont know why!) 
	 * 
	 * ////////////////////////// Example of code
	        String autoConfigUrl = Configuration.get("proxyAutoUrl");
	        String proxyUrl = "http=" + Configuration.get("proxyAddress") + ":" + Configuration.get("proxyPort") + ";";
	        proxyUrl += "https=" + Configuration.get("proxyAddress") + ":" + Configuration.get("proxyPort") + ";";
	        proxyUrl += "ftp=" + Configuration.get("proxyAddress") + ":" + Configuration.get("proxyPort");
	
	        String proxyExclusion = Configuration.get("proxyExclude");
	
	        // Build string
	        // http://stackoverflow.com/questions/1564627/how-to-set-automatic-configuration-script-for-a-dial-up-connection-programmati
	        String hexString = "460000001800000003000000";
	
	        try {
	            // proxy
	            hexString += String.format("%02X000000", proxyUrl.length());
	            hexString += Hex.encodeHexString(proxyUrl.getBytes("UTF-8"));
	
	            // proxy exclusion
	            hexString += String.format("%02X000000", proxyExclusion.length());
	            hexString += Hex.encodeHexString(proxyExclusion.getBytes("UTF-8"));
	            hexString += "00000000";
	
	            // unknown
	            hexString += "01000000";
	
	            // auto configuration script
	            hexString += String.format("%02X000000", autoConfigUrl.length());
	            hexString += Hex.encodeHexString(autoConfigUrl.getBytes("UTF-8"));
	
	            // finalisation of string
	            hexString += "41ee29087b79cf010000000000000000000000000100" +
	                              "0000020000000ac82a86000000000000000000000000000000" +
	                              "00000000000000000000000000000000000000000000000000" +
	                              "00000000000000000000000000000000000000000000000000" +
	                              "00000000000000000000000000000000000000000000000000" +
	                              "00000000000000000000000000000000000000000000000000" +
	                              "0000000000";
	        } catch (UnsupportedEncodingException e) {
	        }
	
	        byte[] data = javax.xml.bind.DatatypeConverter.parseHexBinary(hexString);
	        Advapi32Util.registrySetBinaryValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Connections", "DefaultConnectionSettings", data);
	        Advapi32Util.registrySetBinaryValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Connections", "SavedLegacySettings", data);
	
	        Tools.waitMs(5000);
	    ///////////////////////////////

	 */
	private void setWindowsAutoDetectProxy() {
		try {
			// this key is removed from registry once setting is done
			Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "AutoDetect", 1);
		} catch (Exception e) {
			logger.error("Proxy not set: " + e.getMessage());
		}	
	}

}
