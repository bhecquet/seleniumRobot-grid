package com.infotel.seleniumrobot.grid.tests.servlets;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.aspectj.lang.annotation.After;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.servlets.client.FileServletClient;
import com.infotel.seleniumrobot.grid.servlets.server.FileServlet;

/**
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 24/09/2015
 */
public class FileServletTest extends BaseServletTest {

    private int port;
    private File zipArchive;
    private Server fileServer;
    private File unzippedFile;
    private File unzippedArchive;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
        fileServer = startServerForServlet(new FileServlet(), "/extra/" + FileServlet.class.getSimpleName() + "/*");
        port = ((ServerConnector)fileServer.getConnectors()[0]).getLocalPort();

        zipArchive = createZipArchiveWithTextFile();
    }

    @Test(groups={"grid"})
    public void testUploadFile() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("http://localhost:" + port + "/extra/FileServlet/");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());

        FileInputStream fileInputStream = new FileInputStream(zipArchive);
        InputStreamEntity entity = new InputStreamEntity(fileInputStream);
        httpPost.setEntity(entity);

        CloseableHttpResponse execute = httpClient.execute(httpPost);

        StatusLine statusLine = execute.getStatusLine();
        Assert.assertEquals(statusLine.getStatusCode(), 200);

        try (
            InputStream content = execute.getEntity().getContent()) {
            String directory = IOUtils.toString(content);
            unzippedArchive = new File(directory);
            unzippedFile = new File(directory + "/" + ZIP_FILE_NAME);
        }

        Assert.assertTrue(unzippedFile.exists());

        try (FileInputStream unzippedFileStream = new FileInputStream(unzippedFile)) {
            String contents = IOUtils.toString(unzippedFileStream);
            Assert.assertEquals(contents, "test data");        
        }
    }
    
    /**
     * Write output to a specific folder, relative to current servlet path
     * @throws IOException
     */
    @Test(groups={"grid"})
    public void testUploadFileToLocation() throws IOException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	HttpPost httpPost = new HttpPost("http://localhost:" + port + "/extra/FileServlet/?output=upgrade");
    	httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());
    	
    	FileInputStream fileInputStream = new FileInputStream(zipArchive);
    	InputStreamEntity entity = new InputStreamEntity(fileInputStream);
    	httpPost.setEntity(entity);
    	
    	CloseableHttpResponse execute = httpClient.execute(httpPost);
    	
    	StatusLine statusLine = execute.getStatusLine();
    	Assert.assertEquals(statusLine.getStatusCode(), 200);
    	
    	try (
    		InputStream content = execute.getEntity().getContent()) {
    		String directory = IOUtils.toString(content);
    		unzippedArchive = new File(directory);
            unzippedFile = new File(directory + "/" + ZIP_FILE_NAME);
    		
    		Assert.assertTrue(new File(directory).exists());
    		Assert.assertTrue(new File(directory + File.separator + "test_entry.txt").exists());
    		Assert.assertTrue(directory.contains("upgrade"));
    	}

    }
    
    @Test(groups={"grid"})
    public void testUploadFileWithClient() throws IOException {
    	FileServletClient client = new FileServletClient("localhost", port);
    	String folder = Resources.getResource("flat").getFile().substring(1);
    	String reply = client.upgrade(folder);

		Assert.assertTrue(new File(reply).exists());
		Assert.assertTrue(Paths.get(reply, "flat", "flat1.txt").toFile().exists());
		
		try {
			FileUtils.deleteDirectory(new File(reply));
		} catch (Exception e) {}
    	
    }

    @AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
        deleteIfExists(unzippedFile, unzippedArchive, zipArchive);
        fileServer.stop();
    }
}

