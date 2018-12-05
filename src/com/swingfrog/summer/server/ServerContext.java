package com.swingfrog.summer.server;

import com.swingfrog.summer.config.ServerConfig;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class ServerContext {

	private ServerConfig config;
	private SessionHandlerGroup sessionHandlerGroup;
	private SessionContextGroup sessionContextGroup;
	private EventLoopGroup eventGroup;
	private EventLoopGroup pushGroup;
	
	public ServerContext(ServerConfig config, SessionHandlerGroup sessionHandlerGroup,
			SessionContextGroup sessionContextGroup, EventLoopGroup eventGroup) {
		this.config = config;
		this.sessionHandlerGroup = sessionHandlerGroup;
		this.sessionContextGroup = sessionContextGroup;
		this.eventGroup = eventGroup;
		this.pushGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("ServerPush", true));
	}
	public ServerConfig getConfig() {
		return config;
	}
	public void setConfig(ServerConfig config) {
		this.config = config;
	}
	public SessionHandlerGroup getSessionHandlerGroup() {
		return sessionHandlerGroup;
	}
	public void setSessionHandlerGroup(SessionHandlerGroup sessionHandlerGroup) {
		this.sessionHandlerGroup = sessionHandlerGroup;
	}
	public SessionContextGroup getSessionContextGroup() {
		return sessionContextGroup;
	}
	public void setSessionContextGroup(SessionContextGroup sessionContextGroup) {
		this.sessionContextGroup = sessionContextGroup;
	}
	public EventLoopGroup getEventGroup() {
		return eventGroup;
	}
	public void setEventGroup(EventLoopGroup eventGroup) {
		this.eventGroup = eventGroup;
	}
	public EventLoopGroup getPushGroup() {
		return pushGroup;
	}
}
