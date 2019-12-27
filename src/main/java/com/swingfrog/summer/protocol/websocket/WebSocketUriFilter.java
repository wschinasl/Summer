package com.swingfrog.summer.protocol.websocket;

import com.swingfrog.summer.server.exception.WebSocketUriNoFoundException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

public class WebSocketUriFilter extends SimpleChannelInboundHandler<FullHttpRequest> {

	private String wsUri;
	 
    public WebSocketUriFilter(String wsUri) {
        this.wsUri = wsUri;
    }
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (wsUri.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
        } else {
        	ctx.close();
        	throw new WebSocketUriNoFoundException(request.uri());
        }
	}

}
