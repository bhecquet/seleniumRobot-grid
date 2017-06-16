package com.infotel.seleniumrobot.grid.tasks;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
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

import org.apache.log4j.Logger;

public class ScreenshotTask implements Task {
	
	private static final Logger logger = Logger.getLogger(ScreenshotTask.class);
	
	private String screenshot = null;
	private int width = 0;
	private int height = 0;

	@Override
	public void execute() {
		screenshot = captureDesktopToFile();
		
	}

	 /**
	 * prend une capture d'écran
	 */
	public String captureDesktopToFile() {
		
		if (GraphicsEnvironment.isHeadless()) {
			return null;
		}
	
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice defaultGraphicDevice = ge.getDefaultScreenDevice();
		width = defaultGraphicDevice.getDisplayMode().getWidth();
		height = defaultGraphicDevice.getDisplayMode().getHeight();
		
		// Capture the screen shot of the area of the screen defined by the rectangle
        BufferedImage bi;
		try {
			bi = new Robot().createScreenCapture(new Rectangle(width, height));
			return imgToBase64String(bi, "png");
		} catch (AWTException e) {
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
