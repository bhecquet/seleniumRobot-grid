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
package com.infotel.seleniumrobot.grid.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.infotel.seleniumrobot.grid.NodeStarter;

public class Utils {
	
	private static final Logger logger = Logger.getLogger(NodeStarter.class.getName());
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
	
	/**
	 * Returns the PID of the current node/hub
	 * @return
	 */
	public static long getCurrentPID() {
		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		 
		String jvmName = runtimeBean.getName();
		return Long.valueOf(jvmName.split("@")[0]);

	}
	
	/**
	 * Returns true if the port in paramter is already bound to an other program
	 * @return
	 */
	public static boolean portAlreadyInUse(int port) {
		boolean portTaken = false;
	    ServerSocket socket = null;
	    try {
	        socket = new ServerSocket(port);
	    } catch (IOException e) {
	        portTaken = true;
	    } finally {
	        if (socket != null)
	            try {
	                socket.close();
	            } catch (IOException e) {}
	    }
	    return portTaken;
	}
}
