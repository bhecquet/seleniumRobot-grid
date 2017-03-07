package com.infotel.seleniumrobot.grid.servlets.server;

import com.google.common.io.Files;
import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.commons.FileUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Code imported from sterodium: selenium-grid-extensions
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 22/09/2015
 *
 *         Allows to upload zip archive with resources.
 *         Zip contents will be stored in temporary folder,
 *         absolute path will be returned in response body.
 */

public class FileServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FileServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (!MediaType.OCTET_STREAM.toString().equals(req.getContentType())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content type " + req.getContentType() + " is not supported");
            return;
        }

        try {
	        File tempFile = File.createTempFile("selenium_node", ".zip");
	        try (ServletInputStream inputStream = req.getInputStream();
	             OutputStream outputStream = new FileOutputStream(tempFile)) {
	            IOUtils.copy(inputStream, outputStream);
	        }
	
	        File imagesBaseDir = unZip(tempFile);
	        if (!tempFile.delete()) {
	            throw new IOException("Unable to delete file: " + tempFile);
	        }
	        
	        PrintWriter writer = resp.getWriter();
	        if (req.getParameter("output") != null) {
	        	File copyTo = Paths.get(Utils.getRootdir(), req.getParameter("output").replace("..", "")).toFile();
	        	FileUtils.copyDirectory(imagesBaseDir, copyTo);
	        	writer.write(copyTo.getAbsolutePath());
	        } else {
	        	writer.write(imagesBaseDir.getAbsolutePath());
	        }
        } catch (IOException e) {
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error while handling request: " + e.getMessage());
        }
    }

    private static File unZip(final File zippedFile) throws IOException {
        File outputFolder = Files.createTempDir();
        try (ZipFile zipFile = new ZipFile(zippedFile)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final File entryDestination = new File(outputFolder, entry.getName());
                if (entry.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    entryDestination.mkdirs();
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    entryDestination.getParentFile().mkdirs();
                    final InputStream in = zipFile.getInputStream(entry);
                    final OutputStream out = new FileOutputStream(entryDestination);
                    IOUtils.copy(in, out);
                    IOUtils.closeQuietly(in);
                    out.close();
                }
            }
        }
        return outputFolder;
    }
}
