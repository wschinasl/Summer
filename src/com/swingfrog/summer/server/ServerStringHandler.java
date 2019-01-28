package com.swingfrog.summer.server;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.swingfrog.summer.concurrent.SessionQueueMgr;
import com.swingfrog.summer.concurrent.SingleQueueMgr;
import com.swingfrog.summer.ioc.ContainerMgr;
import com.swingfrog.summer.protocol.SessionRequest;
import com.swingfrog.summer.protocol.SessionResponse;
import com.swingfrog.summer.server.exception.CodeException;
import com.swingfrog.summer.server.exception.SessionException;
import com.swingfrog.summer.server.rpc.RpcClientMgr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;

public class ServerStringHandler extends SimpleChannelInboundHandler<String> {
	
	private static final Logger log = LoggerFactory.getLogger(ServerStringHandler.class);
	private ServerContext serverContext;
	
	public ServerStringHandler(ServerContext serverContext) {
		this.serverContext = serverContext;
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		if (serverContext.getConfig().isAllowAddressEnable()) {
			String address = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
			String[] addressList = serverContext.getConfig().getAllowAddressList();
			boolean allow = false;
			for (int i = 0; i < addressList.length; i ++) {
				if (address.equals(addressList[i])) {
					allow = true;
					break;
				}
			}
			if (!allow) {
				log.warn("not allow {} connect", address);
				ctx.close();
				return;
			}
			log.info("allow {} connect", address);
		}
		serverContext.getSessionContextGroup().createSession(ctx);
		SessionContext sctx = serverContext.getSessionContextGroup().getSessionByChannel(ctx);
		if (!serverContext.getSessionHandlerGroup().accpet(sctx)) {
			log.warn("not accpet clinet {}", sctx);
			ctx.close();
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		SessionContext sctx = serverContext.getSessionContextGroup().getSessionByChannel(ctx);
		log.info("added client {}", sctx);
		serverContext.getSessionHandlerGroup().added(sctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		SessionContext sctx = serverContext.getSessionContextGroup().getSessionByChannel(ctx);
		if (sctx != null) {
			log.info("removed clinet {}", sctx);
			serverContext.getSessionHandlerGroup().removed(sctx);
			serverContext.getSessionContextGroup().destroySession(ctx);
			RpcClientMgr.get().remove(sctx);
			SessionQueueMgr.get().shutdown(sctx);
		}
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg)
			throws Exception {
		SessionContext sctx = serverContext.getSessionContextGroup().getSessionByChannel(ctx);
		long now = Calendar.getInstance().getTimeInMillis();
		long last = sctx.getLastRecvTime();
		sctx.setLastRecvTime(now);
		if ((now - last) < serverContext.getConfig().getColdDownMs()) {
			serverContext.getSessionHandlerGroup().sendTooFastMsg(sctx);
		}
		sctx.setHeartCount(0);
		if ("ping".equals(msg)) {
			ctx.writeAndFlush("pong");
		} else if (msg.startsWith("rpc")) {
			String[] msgs = msg.split("\t");
			RpcClientMgr.get().add(sctx, msgs[1], msgs[2]);
		} else {
			try {
				SessionRequest request = JSON.parseObject(msg, SessionRequest.class);
				if (request.getId() != sctx.getCurrentMsgId()) {
					sctx.setCurrentMsgId(request.getId());
					log.debug("server request {} from {}", msg, sctx);
					if (serverContext.getSessionHandlerGroup().receive(sctx, request)) {
						EventLoopGroup eventLoopGroup = serverContext.getEventGroup();
						Method method = RemoteDispatchMgr.get().getMethod(request);
						if (method != null) {
							String singleQueueName = ContainerMgr.get().getSingleQueueName(method);
							if (singleQueueName != null) {
								eventLoopGroup = SingleQueueMgr.get().getEventLoopGroup(singleQueueName);
							} else {
								if (ContainerMgr.get().isSessionQueue(method)) {
									eventLoopGroup = SessionQueueMgr.get().getEventLoopGroup(sctx);
								}
							}
						}
						eventLoopGroup.execute(()->{
							try {
								String response = RemoteDispatchMgr.get().process(request, sctx).toJSONString();
								log.debug("server response {} to {}", response, sctx);
								ctx.writeAndFlush(response);
							} catch (CodeException ce) {
								log.error(ce.getMessage(), ce);
								String response = SessionResponse.buildError(request, ce).toJSONString();
								log.debug("server response error {} to {}", response, sctx);
								ctx.writeAndFlush(response);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
								Throwable cause = e;
								String response = null;
								for (int i = 0; i < 5; i ++) {
									if ((cause = cause.getCause()) != null) {
										if (cause instanceof CodeException) {
											response = SessionResponse.buildError(request, (CodeException) cause).toJSONString();
											break;
										}
									} else {
										break;
									}
								}
								if (response == null) {
									response = SessionResponse.buildError(request, SessionException.INVOKE_ERROR).toJSONString();									
								}
								log.debug("server response error {} to {}", response, sctx);
								ctx.writeAndFlush(response);
							}
						});
					}
				} else {
					serverContext.getSessionHandlerGroup().repetitionMsg(sctx);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				serverContext.getSessionHandlerGroup().unableParseMsg(sctx);
			}
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		SessionContext sctx = serverContext.getSessionContextGroup().getSessionByChannel(ctx);
		if (cause instanceof TooLongFrameException) {
			serverContext.getSessionHandlerGroup().lengthTooLongMsg(sctx);
		} else {
			log.error(cause.getMessage(), cause);
		}
	}

}
