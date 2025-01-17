package com.infotel.seleniumrobot.grid.servlets.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.net.MediaType;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;

public abstract class GridServlet extends HttpServlet {


	public class ServletResponse {
		public int httpCode;
		public String message;
		public File file;
		public MediaType contentType;

		public ServletResponse(int httpCode, String message) {
			this(httpCode, message, MediaType.PLAIN_TEXT_UTF_8);
		}
		public ServletResponse(int httpCode, String message, MediaType contentType) {
			this.httpCode = httpCode;
			this.message = message;
			this.contentType = contentType;
			this.file = null;
		}
		public ServletResponse(int httpCode, File file, MediaType contentType) {
			this.httpCode = httpCode;
			this.message = null;
			this.file = file;
			this.contentType = contentType;
		}

		public void send(HttpServletResponse resp) {

			if (message == null && file == null) {
				message = "null";
			}

			resp.setStatus(httpCode);
			resp.setHeader("Content-Type", contentType.toString());

			// send binary data
			if (file != null) {
				resp.setContentLengthLong(file.length());
				try (ServletOutputStream outputStream = resp.getOutputStream()) {
					FileUtils.copyFile(file, outputStream);
				} catch (IOException e) {
					e.printStackTrace();
                }

            // send text data
			} else {
				resp.setCharacterEncoding(StandardCharsets.UTF_8.toString());
				try (ServletOutputStream outputStream = resp.getOutputStream()) {
					outputStream.print(message);
					outputStream.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
