package com.infotel.seleniumrobot.grid.servlets.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriverException;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.infotel.seleniumrobot.grid.exceptions.FileUploadException;
import com.infotel.seleniumrobot.grid.utils.ResourceFolder;

/**
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 25/09/2015
 */
public class FileServletClient {
	
	
	public static final String UPGRADE_DIR = "upgrade";
	private static final Logger logger = Logger.getLogger(FileServletClient.class.getName());
	private static final String SERVLET_PATH = "/extra/FileServlet/";
	
	private HttpHost httpHost;
	
	public FileServletClient(String host, int port) {
        this.httpHost = new HttpHost(host, port);
    }	

	public String upgrade(String resourcesPath) {
        logger.debug("Uploading resources from path:" + resourcesPath);

        File zip = addResourcesToZip(resourcesPath);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        HttpPost request = new HttpPost(SERVLET_PATH + "?output=" + UPGRADE_DIR);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());

        try {
            FileInputStream fileInputStream = new FileInputStream(zip);
            InputStreamEntity entity = new InputStreamEntity(fileInputStream);
            request.setEntity(entity);

            CloseableHttpResponse execute = httpClient.execute(httpHost, request);

            int statusCode = execute.getStatusLine().getStatusCode();
            String content = contentAsString(execute);

            if (HttpStatus.SC_OK == statusCode) {
                return content;
            } else {
                throw new FileUploadException("Cannot upload file");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
	public File downloadFile(String url) {
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
  
    	try {
    		File tempFile = File.createTempFile("app", url.substring(url.length() - 4, url.length()));
    		URIBuilder builder = new URIBuilder(url);
    		HttpGet httpGet= new HttpGet(builder.build());
			CloseableHttpResponse execute = httpClient.execute(httpGet);
		    InputStream content = execute.getEntity().getContent();
		    
    		if (execute.getStatusLine().getStatusCode() == 200) {
    			FileUtils.copyInputStreamToFile(content, tempFile);
    			return tempFile;
    		} else {
    			throw new WebDriverException("could not download application from hub");
    		}
		} catch (IOException | URISyntaxException e) {
			throw new WebDriverException("could not download application from hub", e);
		}
	}

    private String contentAsString(CloseableHttpResponse execute) throws IOException {
        InputStream response = execute.getEntity().getContent();
        return IOUtils.toString(response, "utf-8");
    }

    protected File addResourcesToZip(String resourcesPath) {
        ResourceFolder resourceFolder = new ResourceFolder(resourcesPath);
        return resourceFolder.toZip();
    }
}
