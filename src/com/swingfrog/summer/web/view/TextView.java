package com.swingfrog.summer.web.view;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

public class TextView implements WebView {

	private ByteBuf byteBuf;
	
	public TextView(String text) {
		byte[] bytes = text.getBytes();
		byteBuf = Unpooled.buffer(bytes.length);
		byteBuf.writeBytes(bytes);
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
		return "text/plain";
	}

	@Override
	public long getLength() {
		return byteBuf.readableBytes();
	}

	@Override
	public ChunkedInput<ByteBuf> getChunkedInput() {
		return new ChunkedInput<ByteBuf>() {
			
			@Override
			public boolean isEndOfInput() throws Exception {
				return true;
			}

			@Override
			public void close() throws Exception {
				byteBuf.clear();
			}

			@Override
			public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
				return null;
			}

			@Override
			public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
				return byteBuf;
			}

			@Override
			public long length() {
				return byteBuf.readableBytes();
			}

			@Override
			public long progress() {
				return 0;
			}
			
		};
	}

	@Override
	public String toString() {
		return "TextView";
	}
	
}
