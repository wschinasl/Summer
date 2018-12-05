package com.swingfrog.summer.concurrent;

import java.util.HashMap;
import java.util.Map;

import com.swingfrog.summer.server.SessionContext;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class SessionQueueMgr {

private Map<SessionContext, EventLoopGroup> singleQueueMap;
	
	private static class SingleCase {
		public static final SessionQueueMgr INSTANCE = new SessionQueueMgr();
	}
	
	private SessionQueueMgr() {
		singleQueueMap = new HashMap<>();
	}
	
	public static SessionQueueMgr get() {
		return SingleCase.INSTANCE;
	}
	
	public EventLoopGroup getEventLoopGroup(SessionContext key) {
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		EventLoopGroup es = singleQueueMap.get(key);
		if (es == null) {
			synchronized (key) {
				es = singleQueueMap.get(key);
				if (es == null) {
					es = new NioEventLoopGroup(1, new DefaultThreadFactory("SessionQueue", true));
					singleQueueMap.put(key, es);
				}
			}
		}
		return es;
	}
	
	public void shutdown(SessionContext key) {
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		EventLoopGroup es = singleQueueMap.remove(key);
		if (es != null) {
			es.shutdownGracefully();
		}
	}
}
