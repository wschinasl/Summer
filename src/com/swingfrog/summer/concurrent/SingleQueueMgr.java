package com.swingfrog.summer.concurrent;

import java.util.HashMap;
import java.util.Map;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class SingleQueueMgr {

	private Map<Object, EventLoopGroup> singleQueueMap;
	
	private static class SingleCase {
		public static final SingleQueueMgr INSTANCE = new SingleQueueMgr();
	}
	
	private SingleQueueMgr() {
		singleQueueMap = new HashMap<>();
	}
	
	public static SingleQueueMgr get() {
		return SingleCase.INSTANCE;
	}
	
	public EventLoopGroup getEventLoopGroup(Object key) {
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		EventLoopGroup es = singleQueueMap.get(key);
		if (es == null) {
			synchronized (key) {
				es = singleQueueMap.get(key);
				if (es == null) {
					es = new NioEventLoopGroup(1, new DefaultThreadFactory("SingleQueue", true));
					singleQueueMap.put(key, es);
				}
			}
		}
		return es;
	}
	
	public void execute(Object key, Runnable runnable) {
		if (runnable == null) {
			throw new NullPointerException("runnable is null");
		}
		getEventLoopGroup(key).execute(runnable);
	}
}
