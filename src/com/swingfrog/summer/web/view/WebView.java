package com.swingfrog.summer.web.view;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedInput;

public interface WebView {

	public void ready() throws Exception;
	public int getStatus();
	public String getConentType();
	public long getLength() throws IOException;
	public ChunkedInput<ByteBuf> getChunkedInput() throws IOException;
	
}
