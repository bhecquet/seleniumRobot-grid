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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.infotel.seleniumrobot.grid.GridStarter;

public class Utils {

	
	private static final Logger logger = Logger.getLogger(GridStarter.class.getName());
	private static String rootDir = null;
	private static String currentVersion = null;
	
	private Utils() {
		// do nothing
	}
	
	public static String getRootdir() {
		if (rootDir == null) {
			rootDir = getRootDirectory();
		}	
		return rootDir;
	}

	public static Path getDriverDir() {
		return Paths.get(getRootdir(), "drivers");
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
			logger.error(e.getMessage());
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
	
	private static String getVersionFromPom() {
		// Try to get version number from pom.xml (available in Eclipse)
		try {
			String className = Utils.class.getName();
			String classfileName = "/" + className.replace('.', '/') + ".class";
			URL classfileResource = Utils.class.getResource(classfileName);
			if (classfileResource != null) {
				Path absolutePackagePath = Paths.get(classfileResource.toURI()).getParent();
				int packagePathSegments = className.length() - className.replace(".", "").length();
				// Remove package segments from path, plus two more levels
				// for "target/classes", which is the standard location for
				// classes in Eclipse.
				Path path = absolutePackagePath;
				for (int i = 0, segmentsToRemove = packagePathSegments + 2; i < segmentsToRemove; i++) {
					path = path.getParent();
				}
				Path pom = path.resolve("pom.xml");
				try (InputStream is = Files.newInputStream(pom)) {
					Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
					doc.getDocumentElement().normalize();
					String version = (String) XPathFactory.newInstance()
														.newXPath()
														.compile("/project/version")
														.evaluate(doc, XPathConstants.STRING);
					if (version != null) {
						version = version.trim();
						if (!version.isEmpty()) {
							return version;
						}
					}
				}
			} 
			return null;

		} catch (Exception e) {
			return null;
		}
	}
	
	private static String getVersionFromMetaInf() {
		// Try to get version number from maven properties in jar's META-INF
		try (InputStream is = Utils.class.getResourceAsStream("/META-INF/maven/com.infotel.seleniumRobot/seleniumRobot-grid/pom.properties")) {
			if (is != null) {
				Properties p = new Properties();
				p.load(is);
				String version = p.getProperty("version", "").trim();
				if (!version.isEmpty()) {
					return version;
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	private static String getVersionFromManifest() {
		String version = null;
		Package pkg = Utils.class.getPackage();
		if (pkg != null) {
			version = pkg.getImplementationVersion();
			if (version == null) {
				version = pkg.getSpecificationVersion();
			}
		}
		return version;
	}
	
	private static final synchronized String getVersion() {
		
		String version = getVersionFromPom();
		
		if (version == null) {
			version = getVersionFromMetaInf();
		}
		if (version == null) {
			version = getVersionFromManifest();
		}

		version = version == null ? "" : version.trim();
		return version.isEmpty() ? "unknown" : version;
	}

	public static String getCurrentversion() {
		if (currentVersion == null) {
			currentVersion = getVersion();
		}
		return currentVersion;
	}
	
	public static File getGridJar() {
		try {
			URI jarPath = Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			if (jarPath.toString().endsWith(".jar")) {
				return new File(jarPath);
			} else {
				return null;
			}
		} catch (URISyntaxException e) {
			return null;
		}
	}
}
