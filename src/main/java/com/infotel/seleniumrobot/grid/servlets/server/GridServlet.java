package com.infotel.seleniumrobot.grid.servlets.server;

import java.nio.charset.StandardCharsets;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import com.google.common.net.MediaType;

public abstract class GridServlet extends HttpServlet {

	protected void sendOk(HttpServletResponse resp, String message) {
		
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setCharacterEncoding(StandardCharsets.UTF_8.toString());
		try (ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(message);
			outputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
		}
	}
	protected void sendOkJson(HttpServletResponse resp, String message) {
		
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Content-Type", MediaType.JSON_UTF_8.toString());
		resp.setCharacterEncoding(StandardCharsets.UTF_8.toString());
		try (ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(message);
			outputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
		}
	}
	
	protected void sendError(int code, HttpServletResponse resp, String msg) {
		
	    resp.setStatus(code);
	    try (ServletOutputStream outputStream = resp.getOutputStream()) {
			outputStream.print(msg);
			outputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
