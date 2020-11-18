package com.ogp.logger.appender;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.zip.Deflater;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

@Plugin(name = "RamFile", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public final class RamFileAppender extends AbstractAppender {

	private int bufLength = 0;

	private int bufMaxLength = 1 * 1024 * 1024;

	private SimpleDateFormat formatter = new SimpleDateFormat("_yyyy-MM-dd_HH-mm-ss-SSS");

	private String filePattern;

	private ByteArrayOutputStream baos;

	private Long intervalStartTime;

	private Long interval;

	private RamFileAppender(String name, String fileName, String fileSize, String interval, Filter filter,
			Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
		super(name, filter, layout, ignoreExceptions, properties);
		File file = new File(".");
		String curDir = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."));
		if (fileSize != null && !fileSize.isEmpty()) {
			bufMaxLength = Integer.valueOf(fileSize);
		}
		if (interval != null && Long.valueOf(interval) != 0) {
			this.interval = Long.valueOf(interval);
		}
		this.filePattern = curDir + fileName;
		this.baos = new ByteArrayOutputStream(bufMaxLength);
		this.intervalStartTime = System.currentTimeMillis();
	}

	public void append(LogEvent e) {
		String loggerName = e.getLoggerName();
		Message message = e.getMessage();		
		byte[] bytesToWrite = getLayout().toByteArray(e);
		try {
			baos.write(bytesToWrite);
		} catch (IOException e1) {
			LOGGER.error("Cannot write to stream", e1);
		}
		bufLength += bytesToWrite.length;
		bytesToWrite = null;
		boolean lengthConditionMet = bufLength > bufMaxLength;
		boolean timeConditionMet = (interval != null) && ((e.getTimeMillis() - intervalStartTime) > interval);
		boolean frameworkStopConditionMet = (loggerName.contains("BlueprintExtender")) && (e.getThreadName().contains("Framework stop"));
		if (lengthConditionMet || timeConditionMet || frameworkStopConditionMet) {
			String dateString = formatter.format(System.currentTimeMillis());
			String absFileName = filePattern + dateString + ".log.gz";
			byte[] bytesToCompress = baos.toByteArray();
			baos.reset();
			bufLength = 0;
			intervalStartTime = e.getTimeMillis();
			compressIntoFile(bytesToCompress, absFileName);
			bytesToCompress = null;
		}
	}

	@PluginFactory
	public static RamFileAppender createAppender(@PluginAttribute(value = "name") String name,
			@PluginAttribute(value = "fileName") String fileName, @PluginAttribute(value = "fileSize") String fileSize,
			@PluginAttribute(value = "interval") String interval, 
			@PluginElement(value = "Layout") Layout<? extends Serializable> layout,	@PluginElement(value = "Filter") Filter filter) {
		if (name == null) {
			LOGGER.error("No name provided");
			return null;
		}
		if (fileName == null) {
			LOGGER.error("No fileName provided");
			return null;
		}

		return new RamFileAppender(name, fileName, fileSize, interval, filter, layout, true, Property.EMPTY_ARRAY);
	}

	private static void compressIntoFile(byte[] dataToCompress, String absFileName) {
		FileOutputStream fileStream = null;
		RamZipOutputStream zipStream = null;
		try {
			fileStream = new FileOutputStream(absFileName);
			zipStream = new RamZipOutputStream(fileStream);
			zipStream.setLevel(Deflater.BEST_SPEED);
			zipStream.write(dataToCompress);
		} catch (Exception e) {
			LOGGER.error("Cannot compress file", e);
		} finally {
			try {
				if (zipStream != null) {
					zipStream.close();
					zipStream = null;
				}
				if (fileStream != null) {
					fileStream.close();
					fileStream = null;
				}
			} catch (Exception e) {
				LOGGER.error("Cannot close stream", e);
			}
		}
	}

}
