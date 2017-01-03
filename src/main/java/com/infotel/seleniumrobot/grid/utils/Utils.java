package com.infotel.seleniumrobot.grid.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.infotel.seleniumrobot.grid.GridStarter;

public class Utils {
	
	private static final Logger logger = Logger.getLogger(GridStarter.class.getName());
	private static final String rootDir = getRootDirectory();
	private static final Path driverDir = Paths.get(rootDir, "drivers");
	
	public static String getRootdir() {
		return rootDir;
	}

	public static Path getDriverDir() {
		return driverDir;
	}

	private static String getRootDirectory() {

		StringBuilder path = new StringBuilder();
		try {
			String url = URLDecoder.decode(Utils.class.getProtectionDomain().getCodeSource().getLocation().getFile(), "UTF-8" );
			if (url.endsWith(".jar")) {
				path.append((new File(url).getParentFile().getAbsoluteFile().toString() + "/").replace(File.separator, "/"));
			} else {				
				path.append((new File(url).getParentFile().getParentFile().getAbsoluteFile().toString() + "/").replace(File.separator, "/"));
			}
		} catch (UnsupportedEncodingException e) {
			logger.severe(e.getMessage());
		}
		return path.toString();
	}
}
