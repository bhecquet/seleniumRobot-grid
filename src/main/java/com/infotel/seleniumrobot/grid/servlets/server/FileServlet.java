package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.zip.commons.FileUtils;

import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.utils.Utils;

/**
 * Code imported from sterodium: selenium-grid-extensions
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 22/09/2015
 *
 *         Allows to upload zip archive with resources.
 *         Zip contents will be stored in temporary folder,
 *         absolute path will be returned in response body.
 */

public class FileServlet extends GridServlet {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String UPLOAD_DIR = "upload";
	private static final Integer KEEP_DURATION = 24;
	public static final String FILE_PREFIX = "file:";
    private static final Logger logger = LogManager.getLogger(FileServlet.class.getName());
    
    /**
     * receive file uploaded from client and copy it to upload or upgrade directory according to the usage of this servlet
     * 
     * POST a zip file content to `/grid/admin/FileServlet` with `output` parameter will unzip the file to the hub. 
     * `output` values can be 'upgrade' to place file to `<grid_root>/upgrade` folder, 
     * or any path value which will unzip file under `<grid_root>/upload/<your_path>/<some_random_id>`. 
     * This returns the path where files where unzipped
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    	// removed files older than 24 hours
    	removeOldFiles();
    	
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
	
	        File imagesBaseDir = Utils.unZip(tempFile);
	        if (!tempFile.delete()) {
	            throw new IOException("Unable to delete file: " + tempFile);
	        }
	        
	        PrintWriter writer = resp.getWriter();
	        String subDir;
	        if (req.getParameter("output") != null) {
	        	subDir = UPLOAD_DIR + "/" + req.getParameter("output") + "/" + UUID.randomUUID();
	        	
	        } else {
	        	subDir = UPLOAD_DIR + "/temp";
	        }
	        File copyTo = Paths.get(Utils.getRootdir(), subDir).toFile();
	        FileUtils.copyDirectory(imagesBaseDir, copyTo);
	        logger.info("file uploaded to " + copyTo.getAbsolutePath());
	        
	        if (req.getParameter("localPath") != null && "true".equalsIgnoreCase(req.getParameter("localPath"))) {
	        	writer.write(new File(subDir).getAbsolutePath().replace("\\", "/"));
	        } else {
	        	writer.write(FILE_PREFIX + subDir.replace("\\", "/"));
	        }
        } catch (IOException e) {
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error while handling request: " + e.getMessage());
        }
    }
    
    /**
     * Allow downloading of files in upload folder
     * GET `/grid/admin/FileServlet?file=file:<filePath>` will download file present in the `upload` directory only. The path is relative to this folder
     * GET `/grid/admin/FileServlet/<filePath>` will download file present in the `upload` directory only. The path is relative to this folder. This is used by mobile tests to make application file available to appium.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try (
	        ServletOutputStream outputStream = resp.getOutputStream()) {

			String fileLocation = req.getParameter("file");
			if (fileLocation != null) {
				if (!fileLocation.startsWith(FILE_PREFIX)) {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "file parameter must begin with 'file:'");
				}
			} else {
				fileLocation = FILE_PREFIX + req.getPathInfo().substring(1);
			}
			
			// only files in upload folder will be allowed
			if (!fileLocation.startsWith(FILE_PREFIX + UPLOAD_DIR + "/")) {
				resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "file not found: " + fileLocation);
			}
			
			File toDownload = Paths.get(Utils.getRootdir(), fileLocation.replace(FILE_PREFIX, "")).toFile();
			if (!toDownload.isFile()) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, fileLocation + " not found");
			}

			resp.addHeader("content-disposition", String.format("attachment; filename=\"%s\"", toDownload.getName()));
			FileUtils.copy(toDownload, resp.getOutputStream());
		
		} catch (IOException e) {
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error while handling request: " + e.getMessage());
        }
    }
    
    /**
     * Remove files / folders older than 1 day
     */
    private void removeOldFiles() {
    	
    	if (!Paths.get(Utils.getRootdir(), UPLOAD_DIR).toFile().isDirectory()) {
    		return;
    	}
    	
    	try {
    		Instant deleteDate = Instant.now().minus(Duration.ofHours(KEEP_DURATION));
  
			Files.walk(Paths.get(Utils.getRootdir(), UPLOAD_DIR), FileVisitOption.FOLLOW_LINKS)
				.filter(path -> path.toFile().lastModified() < deleteDate.toEpochMilli())
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
		} catch (IOException e) {
		}
    	
    }
}
