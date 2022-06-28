package com.infotel.seleniumrobot.grid.tasks;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScreenshotTask implements Task {
	
	private static final Logger logger = LogManager.getLogger(ScreenshotTask.class);
	
	private String screenshot = null;
	private int width = 0;
	private int height = 0;

	@SuppressWarnings("unchecked")
	@Override
	public ScreenshotTask execute() {
		screenshot = captureDesktopToFile();

		return this;
	}

	 /**
	 * take desktop screenshot
	 */
	public String captureDesktopToFile() {
		
		if (GraphicsEnvironment.isHeadless()) {
			return null;
		}
	
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//		GraphicsDevice defaultGraphicDevice = ge.getDefaultScreenDevice();
//		width = defaultGraphicDevice.getDisplayMode().getWidth();
//		height = defaultGraphicDevice.getDisplayMode().getHeight();
//		
//		
		Rectangle screenRect = new Rectangle(0, 0, 0, 0);
		for (GraphicsDevice gd : ge.getScreenDevices()) {
		    screenRect = screenRect.union(gd.getDefaultConfiguration().getBounds());
		}
		
		// Capture the screen shot of the area of the screen defined by the rectangle
        BufferedImage bi;
		try {
			bi = new Robot().createScreenCapture(screenRect);
			PointerInfo pointerInfo = MouseInfo.getPointerInfo();
			Point mouseCoords = pointerInfo.getLocation();
			mouseCoords.setLocation(mouseCoords.x - screenRect.x, mouseCoords.y - screenRect.y);
			
			Graphics graphic = bi.getGraphics();
			graphic.setColor(Color.red);
			graphic.drawLine(mouseCoords.x, mouseCoords.y, mouseCoords.x, mouseCoords.y + 12);
			graphic.drawLine(mouseCoords.x, mouseCoords.y, mouseCoords.x + 10, mouseCoords.y + 10);
			graphic.drawLine(mouseCoords.x, mouseCoords.y, mouseCoords.x + 8, mouseCoords.y + 17);
			graphic.dispose();
			
			
			
			return imgToBase64String(bi, "png");
		} catch (AWTException | HeadlessException e) {
			logger.warn("Cannot capture image", e);
			return null;
		}
	}
	
	private String imgToBase64String(final RenderedImage img, final String formatName) {
	    final ByteArrayOutputStream os = new ByteArrayOutputStream();
	    try {
	        ImageIO.write(img, formatName, Base64.getEncoder().wrap(os));
	        return os.toString(StandardCharsets.ISO_8859_1.name());
	    } catch (final IOException ioe) {
	        throw new UncheckedIOException(ioe);
	    }
	}
	
	public String getScreenshot() {
		return screenshot;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
}
