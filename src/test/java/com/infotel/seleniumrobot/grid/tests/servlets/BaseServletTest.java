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
package com.infotel.seleniumrobot.grid.tests.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServlet;

import org.apache.commons.io.IOUtils;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.servlet.ServletContextHandler;
import org.seleniumhq.jetty9.servlet.ServletHolder;
import org.testng.Assert;

import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;

/**
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 24/09/2015
 */
public class BaseServletTest extends BaseMockitoTest {
	
	public static final String ZIP_FILE_NAME = "test_entry.txt";

    protected Server startServerForServlet(HttpServlet servlet, String path) throws Exception {
        Server server = new Server(0);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(servlet), path);
        server.start();

        return server;
    }
    
    protected File createZipArchiveWithTextFile() throws IOException {
        final File zipArchive = File.createTempFile("temp_zip_", ".zip");
        try (
                final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipArchive))) {
            ZipEntry e = new ZipEntry(ZIP_FILE_NAME);
            out.putNextEntry(e);
            IOUtils.write("test data", out);
            out.closeEntry();
        }
        return zipArchive;
    }

    protected void deleteIfExists(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

}
