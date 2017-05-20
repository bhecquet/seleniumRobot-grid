package com.infotel.seleniumrobot.grid.tests.servlets;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import com.infotel.seleniumrobot.grid.utils.Utils;
import com.seleniumtests.util.helper.WaitHelper;

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
    private HttpHost serverHost;

    @BeforeMethod(groups={"grid"})
    public void setUp() throws Exception {
        fileServer = startServerForServlet(new FileServlet(), "/extra/" + FileServlet.class.getSimpleName() + "/*");
        serverHost = new HttpHost("localhost", ((ServerConnector)fileServer.getConnectors()[0]).getLocalPort());
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
            Assert.assertTrue(directory.contains("file:" + FileServlet.UPLOAD_DIR + "/temp"));
            unzippedArchive = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, "")).toFile();
            unzippedFile = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, ""), ZIP_FILE_NAME).toFile();
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
    	
    	HttpPost httpPost = new HttpPost("http://localhost:" + port + "/extra/FileServlet/?output=aFolder");
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
    		unzippedArchive = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, "")).toFile();
            unzippedFile = Paths.get(Utils.getRootdir(), directory.replace(FileServlet.FILE_PREFIX, ""), ZIP_FILE_NAME).toFile();
    		
    		Assert.assertTrue(unzippedArchive.exists());
    		Assert.assertTrue(unzippedFile.exists());
    		Assert.assertTrue(directory.contains("aFolder/"));
    	}
    }
    
    @Test(groups={"grid"})
    public void testUploadFileWithClient() throws IOException {
    	FileServletClient client = new FileServletClient("localhost", port);
    	String folder = Resources.getResource("flat").getFile().substring(1);
    	String reply = client.upgrade(folder);
    	
    	Assert.assertEquals(FileServlet.FILE_PREFIX + FileServletClient.UPGRADE_DIR, reply);

    	File output = Paths.get(Utils.getRootdir(), reply.replace(FileServlet.FILE_PREFIX, "")).toFile();
		Assert.assertTrue(output.exists());
		
		File outFile = Paths.get(Utils.getRootdir(), reply.replace(FileServlet.FILE_PREFIX, ""), "flat", "flat1.txt").toFile();
		Assert.assertTrue(outFile.exists());
		
		try {
			FileUtils.deleteDirectory(new File(reply));
		} catch (Exception e) {}
    }
    
    /**
     * Download text file
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadTextFile() throws ClientProtocolException, IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	// prepare document to download
    	FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello");
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/extra/FileServlet/");
    	builder.setParameter("file", "file:upload/text.txt");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	String content = IOUtils.toString(execute.getEntity().getContent());
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 200, content);
        Assert.assertEquals(content, "hello");

    }
    
    /**
     * Download text file with url (no file parameter): http://<host>:4444/extra/FileServlet/upload/text.txt
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadTextFileWithUrl() throws ClientProtocolException, IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	// prepare document to download
    	FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello");
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/extra/FileServlet/upload/text.txt");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	String content = IOUtils.toString(execute.getEntity().getContent());
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 200, content);
        Assert.assertEquals(content, "hello");
    }
    
    /**
     * Download text file with client
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadTextFileWithClient() throws ClientProtocolException, IOException, URISyntaxException {
 
    	// prepare document to download
    	FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello");
    	
    	FileServletClient client = new FileServletClient("localhost", port);
    	File downloadedFile = client.downloadFile(String.format("http://localhost:%d/extra/FileServlet/?file=file:upload/text.txt", port));

    	Assert.assertEquals(FileUtils.readFileToString(downloadedFile), "hello");
    }
    
    /**
     * Download binary file
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadBinaryFile() throws ClientProtocolException, IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	// prepare document to download
    	FileUtils.moveFile(createZipArchiveWithTextFile(), Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "file.zip").toFile());
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/extra/FileServlet/");
    	builder.setParameter("file", "file:upload/file.zip");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	
    	
    	try (
    		InputStream content = execute.getEntity().getContent()) {
    		Assert.assertEquals(execute.getStatusLine().getStatusCode(), 200, "Error while getting file");
    		File zipFile = Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "data.zip").toFile();
    	    FileUtils.copyInputStreamToFile(content, zipFile);
    	    WaitHelper.waitForMilliSeconds(500);
    		File unzipped = Utils.unZip(zipFile);
    		Assert.assertTrue(Paths.get(unzipped.getAbsolutePath(), "test_entry.txt").toFile().exists());
    		Assert.assertEquals(FileUtils.readFileToString(Paths.get(unzipped.getAbsolutePath(), "test_entry.txt").toFile()), "test data");
    	}
    }
    
    /**
     * Test error when file: pattern not set
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadTextFileWithWrongPattern() throws ClientProtocolException, IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	// prepare document to download
    	FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello");
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/extra/FileServlet/");
    	builder.setParameter("file", "upload/text.txt");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 400);
    }
    
    /**
     * Test error when file is not there
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadFileNotFound() throws ClientProtocolException, IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/extra/FileServlet/");
    	builder.setParameter("file", "file:upload/text.txt");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 404);
    }
    
    /**
     * Test error when file is not there
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadFileNotInUpload() throws ClientProtocolException, IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	// prepare document to download
    	FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "text.txt").toFile(), "hello");
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/extra/FileServlet/");
    	builder.setParameter("file", "file:text.txt");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 406);
    }
    
    /**
     * Download text file in subdirectory
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(groups={"grid"})
    public void testDownloadTextFileInSubdirectory() throws ClientProtocolException, IOException, URISyntaxException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	// prepare document to download
    	FileUtils.writeStringToFile(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR, "test", "text.txt").toFile(), "hello");
    	
    	URIBuilder builder = new URIBuilder();
    	builder.setPath("/extra/FileServlet/");
    	builder.setParameter("file", "file:upload/test/text.txt");
    	
    	HttpGet httpGet= new HttpGet(builder.build());
    	CloseableHttpResponse execute = httpClient.execute(serverHost, httpGet);
    	Assert.assertEquals(execute.getStatusLine().getStatusCode(), 200);
    	
    	try (
        	InputStream content = execute.getEntity().getContent()) {
        		Assert.assertEquals(IOUtils.toString(content), "hello");
    
    	}
    }

    @AfterMethod(groups={"grid"})
    public void tearDown() throws Exception {
        deleteIfExists(unzippedFile, unzippedArchive, zipArchive);
        FileUtils.deleteDirectory(Paths.get(Utils.getRootdir(), FileServlet.UPLOAD_DIR).toFile());
        FileUtils.deleteDirectory(Paths.get(Utils.getRootdir(), FileServletClient.UPGRADE_DIR).toFile());
        
        fileServer.stop();
    }
}

