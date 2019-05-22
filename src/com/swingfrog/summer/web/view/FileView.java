package com.swingfrog.summer.web.view;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.activation.MimetypesFileTypeMap;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedInput;

public class FileView implements WebView {

	private RandomAccessFile file;
	private String contentType;
	
	public FileView(String fileName) throws IOException {
		file = new RandomAccessFile(fileName, "r");
		contentType = Files.probeContentType(Paths.get(fileName));
		if (contentType == null) {
			MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
			contentType = mimeTypesMap.getContentType(fileName);
		}
	}
	
	@Override
	public void ready() {

	}
	
	@Override
	public int getStatus() {
		return 200;
	}

	@Override
	public String getConentType() {
		return contentType;
	}

	@Override
	public long getLength() throws IOException {
		return file.length();
	}

	@Override
	public ChunkedInput<ByteBuf> getChunkedInput() throws IOException {
		return new ChunkedFile(file);
	}
	
	@Override
	public String toString() {
		return "FileView";
	}

}
