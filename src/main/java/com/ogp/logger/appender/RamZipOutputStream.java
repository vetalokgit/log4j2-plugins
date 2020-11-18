package com.ogp.logger.appender;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class RamZipOutputStream extends GZIPOutputStream {

	public RamZipOutputStream(OutputStream out) throws IOException {
		super(out);
	}

	public void setLevel(int level) {
		def.setLevel(level);
	}
}
