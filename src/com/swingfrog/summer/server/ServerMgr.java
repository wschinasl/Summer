package com.swingfrog.summer.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.config.ConfigMgr;
import com.swingfrog.summer.config.ServerConfig;
import com.swingfrog.summer.ioc.ContainerMgr;

import io.netty.channel.EventLoopGroup;
import javassist.NotFoundException;

public class ServerMgr {

	private static final Logger log = LoggerFactory.getLogger(ServerMgr.class);
	private Server server;
	private Map<String, Server> serverMap;
	
	private static class SingleCase {
		public static final ServerMgr INSTANCE = new ServerMgr();
	}
	
	private ServerMgr() {
		serverMap = new HashMap<>();
	}
	
	public static ServerMgr get() {
		return SingleCase.INSTANCE;
	}
	
	public void init() throws NotFoundException {
		log.info("server init...");
		ServerConfig serverConfig = ConfigMgr.get().getServerConfig();
		if (serverConfig == null) {
			throw new NullPointerException("serverConfig is null");
		}
		server = Server.create(serverConfig);
		Iterator<Class<?>> iteratorHandler = ContainerMgr.get().iteratorHandlerList();
		while (iteratorHandler.hasNext()) {
			Class<?> clazz = iteratorHandler.next();
			log.info("server register session handler {}", clazz.getSimpleName());
			server.addSessionHandler((SessionHandler) ContainerMgr.get().getDeclaredComponent(clazz));
		}
		ServerConfig[] minorConfigs = ConfigMgr.get().getMinorConfigs();
		if (minorConfigs != null && minorConfigs.length > 0) {
			for (ServerConfig sc : minorConfigs) {
				Server s = Server.createMinor(sc, server.getBossGroup(), server.getWorkerGroup(), server.getEventLoopGroup());
				Iterator<Class<?>> sciteratorHandler = ContainerMgr.get().iteratorHandlerList(sc.getServerName());
				if (sciteratorHandler != null) {
					while (sciteratorHandler.hasNext()) {
						Class<?> clazz = sciteratorHandler.next();
						log.info("server [{}] register session handler {}", sc.getServerName(), clazz.getSimpleName());
						s.addSessionHandler((SessionHandler) ContainerMgr.get().getDeclaredComponent(clazz));
					}
				}
				serverMap.put(sc.getServerName(), s);
			}
		}
		RemoteDispatchMgr.get().init();
	}
	
	public void launch() {
		log.info("server launch...");
		server.launch();
		for(Entry<String, Server> entry : serverMap.entrySet()) {
			log.info("server [{}] launch...", entry.getKey());
			entry.getValue().launch();
		}
	}
	
	public ServerPush getServerPush() {
		return server.getServerPush();
	}
	
	public void closeSession(SessionContext sctx) {
		server.closeSession(sctx);
	}
	
	public EventLoopGroup getEventLoopGroup() {
		return server.getEventLoopGroup();
	}
	
	public ServerPush getServerPush(String serverName) {
		return serverMap.get(serverName).getServerPush();
	}

	public void closeSession(String serverName, SessionContext sctx) {
		serverMap.get(serverName).closeSession(sctx);
	}
	
	public EventLoopGroup getEventLoopGroup(String serverName) {
		return serverMap.get(serverName).getEventLoopGroup();
	}
	
}
