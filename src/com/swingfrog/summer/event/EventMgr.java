package com.swingfrog.summer.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.annotation.BindEvent;
import com.swingfrog.summer.ioc.ContainerMgr;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class EventMgr {

	private static final Logger log = LoggerFactory.getLogger(EventMgr.class);
	private Map<Method, Class<?>> methodMap;
	private Map<String, List<EventMethod>> eventMap;
	private NioEventLoopGroup eventGroup;
	
	private static class SingleCase {
		public static final EventMgr INSTANCE = new EventMgr();
	}
	
	private EventMgr() {
		methodMap = new HashMap<>();
		eventMap = new HashMap<>();
		eventGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("EventHandler", true));
	}
	
	public static EventMgr get() {
		return SingleCase.INSTANCE;
	}
	
	public void init() {
		Iterator<Class<?>> ite = ContainerMgr.get().iteratorEventList();
		while (ite.hasNext()) {
			Class<?> clazz = ite.next();
			Method[] methods = clazz.getDeclaredMethods();
			for (Method method : methods) {
				BindEvent event = method.getDeclaredAnnotation(BindEvent.class);
				if (event != null) {
					methodMap.put(method, clazz);
					List<EventMethod> eventList = eventMap.get(event.value());
					if (eventList == null) {
						eventList = new ArrayList<>();
						eventMap.put(event.value(), eventList);
					}
					eventList.add(new EventMethod(method, event.index()));
				}
			}
		}
		for (String eventName : eventMap.keySet()) {
			List<EventMethod> eventList = eventMap.get(eventName);
			Collections.sort(eventList, (a, b)->{
				return a.getIndex() - b.getIndex();
			});
			log.info("event name {}", eventName);
			for (EventMethod event : eventList) {
				Class<?> clazz = methodMap.get(event.getMethod());
				log.info("event register event handler {}.{} index[{}]", clazz.getSimpleName(), event.getMethod().getName(), event.getIndex());
			}
		}
	}
	
	private void dispatch(String eventName, Object ...args) {
		log.debug("dispatch event[{}]", eventName);
		List<EventMethod> eventList = eventMap.get(eventName);
		if (eventList == null) {
			log.warn("event handler {} not exist", eventName);
		} else {
			for (EventMethod event : eventList) {
				Class<?> clazz = methodMap.get(event.getMethod());
				Object obj = ContainerMgr.get().getDeclaredComponent(clazz);
				log.debug("dispatch event[{}] invoke {}.{}", eventName, clazz.getSimpleName(), event.getMethod().getName());
				try {
					if (event.getMethod().invoke(obj, args) != null) {
						break;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}
	
	public void syncDispatch(String eventName, Object ...args) {
		dispatch(eventName, args);
	}
	
	public void asyncDispatch(String eventName, Object ...args) {
		eventGroup.execute(()->{
			dispatch(eventName, args);
		});
	}
	
	private class EventMethod {
		private Method method;
		private int index;
		public EventMethod(Method method, int index) {
			this.method = method;
			this.index = index;
		}
		public Method getMethod() {
			return method;
		}
		public int getIndex() {
			return index;
		}
	}
	
}
