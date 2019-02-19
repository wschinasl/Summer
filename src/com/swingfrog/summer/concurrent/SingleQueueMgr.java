package com.swingfrog.summer.concurrent;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.EventLoopGroup;

public class SingleQueueMgr {

	private EventLoopGroup eventLoopGroup;
	private Map<Object, RunnableQueue> singleQueueMap;
	
	private static class SingleCase {
		public static final SingleQueueMgr INSTANCE = new SingleQueueMgr();
	}
	
	private SingleQueueMgr() {
		singleQueueMap = new HashMap<>();
	}
	
	public static SingleQueueMgr get() {
		return SingleCase.INSTANCE;
	}
	
	public void init(EventLoopGroup eventLoopGroup) {
		this.eventLoopGroup = eventLoopGroup;
	}
	
	public RunnableQueue getRunnableQueue(Object key) {
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		RunnableQueue rq = singleQueueMap.get(key);
		if (rq == null) {
			synchronized (key) {
				rq = singleQueueMap.get(key);
				if (rq == null) {
					rq = RunnableQueue.build();
					singleQueueMap.put(key, rq);
				}
			}
		}
		return rq;
	}
	
	public void execute(Object key, Runnable runnable) {
		if (runnable == null) {
			throw new NullPointerException("runnable is null");
		}
		getRunnableQueue(key).getQueue().add(runnable);
		next(key);
	}
	
	public void next(Object key) {
		RunnableQueue rq = getRunnableQueue(key);
		if (rq.getState().compareAndSet(true, false)) {
			Runnable runnable = rq.getQueue().poll();
			if (runnable != null) {
				eventLoopGroup.execute(()->{
					try {						
						runnable.run();
					} finally {						
						finish(key);
					}
				});
			} else {
				rq.getState().compareAndSet(false, true);
			}
		}
	}
	
	public void finish(Object key) {
		RunnableQueue rq = getRunnableQueue(key);
		rq.getState().compareAndSet(false, true);
		next(key);
	}
}
