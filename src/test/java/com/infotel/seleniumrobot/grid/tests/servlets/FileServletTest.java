package com.infotel.seleniumrobot.grid.tests.servlets;


import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.servlets.server.FileServlet;
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.helper.WaitHelper;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * @author Alexey Nikolaenko alexey@tcherezov.com
 * Date: 24/09/2015
 */
public class FileServletTest extends BaseServletTest {

    private int port;
    private File zipArchive;
    private Server fileServer;
    private File unzippedFile;
    private File unzippedArchive;

    @BeforeMethod(groups = {"grid"})
    public void setUp() throws Exception {
        fileServer = startServerForServlet(new FileServlet(), "/extra/" + FileServlet.class.getSimpleName() + "/*");
        port = ((ServerConnector) fileServer.getConnectors()[0]).getLocalPort();

        zipArchive = createZipArchiveWithTextFile();
    }

    @Test(groups = {"grid"})
    public void testUploadFile() throws IOException {
        HttpResponse<String> response = Unirest.post("http://localhost:" + port + "/extra/FileServlet/")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString())
                .body(new FileInputStream(zipArchive))
                .asString();

        Assert.assertEquals(response.getStatus(), 200);

        String directory = response.getBody();
        Assert.assertTrue(directory.contains("file:" + FileServlet.UPLOAD_DIR + "/temp"));
        unzippedArchive = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, "")).toFile();
        unzippedFile = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, ""), ZIP_FILE_NAME).toFile();

        Assert.assertTrue(unzippedFile.exists());

        try (FileInputStream unzippedFileStream = new FileInputStream(unzippedFile)) {
            String contents = IOUtils.toString(unzippedFileStream, StandardCharsets.UTF_8);
            Assert.assertEquals(contents, "test data");
        }
    }

    /**
     * Write output to a specific folder, relative to current servlet path
     */
    @Test(groups = {"grid"})
    public void testUploadFileToLocation() throws IOException {

        HttpResponse<String> response = Unirest.post("http://localhost:" + port + "/extra/FileServlet/?output=aFolder")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString())
                .body(new FileInputStream(zipArchive))
                .asString();
        Assert.assertEquals(response.getStatus(), 200);

        String directory = response.getBody();
        unzippedArchive = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, "")).toFile();
        unzippedFile = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, ""), ZIP_FILE_NAME).toFile();

        Assert.assertTrue(unzippedArchive.exists());
        Assert.assertTrue(unzippedFile.exists());
        Assert.assertTrue(directory.contains("aFolder/"));
    }

    /**
     * Check that with "localPath=true" parameter, servlet returns the full local path where file are copied
     */
    @Test(groups = {"grid"})
    public void testUploadFileToLocationWithLocalReturn() throws IOException {
        HttpResponse<String> response = Unirest.post("http://localhost:" + port + "/extra/FileServlet/?output=aFolder&localPath=true")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString())
                .body(new FileInputStream(zipArchive))
                .asString();
        Assert.assertEquals(response.getStatus(), 200);

        String directory = response.getBody();
        Assert.assertFalse(directory.startsWith(FileServlet.FILE_PREFIX));
        Assert.assertTrue(new File(directory).isDirectory());

    }

    @Test(groups = {"grid"})
    public void testUploadFileToLocationWithoutLocalReturn() throws IOException {
        HttpResponse<String> response = Unirest.post("http://localhost:" + port + "/extra/FileServlet/?output=aFolder&localPath=false")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString())
                .body(new FileInputStream(zipArchive))
                .asString();
        Assert.assertEquals(response.getStatus(), 200);

        String directory = response.getBody();
        Assert.assertTrue(directory.startsWith(FileServlet.FILE_PREFIX));
    }

    /**
     * Download text file
     */
    @Test(groups = {"grid"})
    public void testDownloadTextFile() throws IOException {

        // prepare document to download
        FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello", StandardCharsets.UTF_8);

        HttpResponse<String> response = Unirest.get("http://localhost:" + port + "/extra/FileServlet/?file=file:upload/text.txt")
                .asString();
        Assert.assertEquals(response.getStatus(), 200);

        String content = response.getBody();
        Assert.assertEquals(content, "hello");

    }

    /**
     * Download text file with url (no file parameter): http://<host>:4444/extra/FileServlet/upload/text.txt
     */
    @Test(groups = {"grid"})
    public void testDownloadTextFileWithUrl() throws IOException {

        // prepare document to download
        FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello", StandardCharsets.UTF_8);

        HttpResponse<String> response = Unirest.get("http://localhost:" + port + "/extra/FileServlet/upload/text.txt")
                .asString();
        Assert.assertEquals(response.getStatus(), 200);

        String content = response.getBody();
        Assert.assertEquals(content, "hello");
    }

    /**
     * Download binary file
     */
    @Test(groups = {"grid"})
    public void testDownloadBinaryFile() throws IOException {

        // prepare document to download
        FileUtils.moveFile(createZipArchiveWithTextFile(), Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "file.zip").toFile());
        File zipFile = Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "data.zip").toFile();

        HttpResponse<File> response = Unirest.get("http://localhost:" + port + "/extra/FileServlet/?file=file:upload/file.zip")
                .asFile(zipFile.getAbsolutePath());
        Assert.assertEquals(response.getStatus(), 200);

        WaitHelper.waitForMilliSeconds(500);
        File unzipped = Utils.unZip(zipFile);
        Assert.assertTrue(Paths.get(unzipped.getAbsolutePath(), "test_entry.txt").toFile().exists());
        Assert.assertEquals(FileUtils.readFileToString(Paths.get(unzipped.getAbsolutePath(), "test_entry.txt").toFile(), StandardCharsets.UTF_8), "test data");

    }

    /**
     * Test error when file: pattern not set
     */
    @Test(groups = {"grid"})
    public void testDownloadTextFileWithWrongPattern() throws IOException {

        // prepare document to download
        FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello", StandardCharsets.UTF_8);

        HttpResponse<String> response = Unirest.get("http://localhost:" + port + "/extra/FileServlet/?file=upload/text.txt")
                .asString();
        Assert.assertEquals(response.getStatus(), 400);

    }

    /**
     * Test error when file is not there
     */
    @Test(groups = {"grid"})
    public void testDownloadFileNotFound() {

        HttpResponse<String> response = Unirest.get("http://localhost:" + port + "/extra/FileServlet/?file=file:upload/text.txt")
                .asString();
        Assert.assertEquals(response.getStatus(), 404);
    }

    /**
     * Test error when file is not there
     */
    @Test(groups = {"grid"})
    public void testDownloadFileNotInUpload() throws IOException {

        // prepare document to download
        FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello", StandardCharsets.UTF_8);

        HttpResponse<String> response = Unirest.get("http://localhost:" + port + "/extra/FileServlet/?file=file:text.txt")
                .asString();
        Assert.assertEquals(response.getStatus(), 406);
    }

    /**
     * Download text file in subdirectory
     */
    @Test(groups = {"grid"})
    public void testDownloadTextFileInSubdirectory() throws IOException {

        // prepare document to download
        FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "test", "text.txt").toFile(), "hello", StandardCharsets.UTF_8);

        HttpResponse<String> response = Unirest.get("http://localhost:" + port + "/extra/FileServlet/?file=file:upload/test/text.txt")
                .asString();
        Assert.assertEquals(response.getStatus(), 200);

        String content = response.getBody();
        Assert.assertEquals(content, "hello");
    }

    @AfterMethod(groups = {"grid"})
    public void tearDown() throws Exception {
        deleteIfExists(unzippedFile, unzippedArchive, zipArchive);
        FileUtils.deleteDirectory(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR).toFile());

        fileServer.stop();
    }
}

