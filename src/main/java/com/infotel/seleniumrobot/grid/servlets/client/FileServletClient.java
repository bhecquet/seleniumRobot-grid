package com.infotel.seleniumrobot.grid.servlets.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.base.Throwables;
import com.infotel.seleniumrobot.grid.exceptions.FileUploadException;
import com.infotel.seleniumrobot.grid.utils.ResourceFolder;

/**
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 25/09/2015
 */
public class FileServletClient {
	
	private static final Logger logger = Logger.getLogger(FileServletClient.class.getName());
	private static final String SERVLET_PATH = "/extra/FileServlet/";
	
	private final HttpHost httpHost;
	
	public FileServletClient(String host, int port) {
        this.httpHost = new HttpHost(host, port);
    }

	public String upload(String resourcesPath) {
        logger.fine("Uploading resources from path:" + resourcesPath);

        File zip = addResourcesToZip(resourcesPath);

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost request = new HttpPost(SERVLET_PATH);
        request.setHeader("Content-Type", "application/octet-stream");

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
            throw Throwables.propagate(e);
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
