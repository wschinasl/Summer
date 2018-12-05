package com.swingfrog.summer.server;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.config.ConfigMgr;
import com.swingfrog.summer.config.ServerConfig;
import com.swingfrog.summer.ioc.ContainerMgr;

import javassist.NotFoundException;

public class ServerMgr {

	private static final Logger log = LoggerFactory.getLogger(ServerMgr.class);
	private Server server;
	
	private static class SingleCase {
		public static final ServerMgr INSTANCE = new ServerMgr();
	}
	
	private ServerMgr() {
		
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
		server = new Server(serverConfig);
		Iterator<Class<?>> iteratorHandler = ContainerMgr.get().iteratorHandlerList();
		while (iteratorHandler.hasNext()) {
			Class<?> clazz = iteratorHandler.next();
			log.info("server register session handler {}", clazz.getSimpleName());
			server.addSessionHandler((SessionHandler) ContainerMgr.get().getDeclaredComponent(clazz));
		}
		RemoteDispatchMgr.get().init();
	}
	
	public void launch() {
		log.info("server launch...");
		server.launch();
	}
	
	public ServerPush getServerPush() {
		return server.getServerPush();
	}
	
	public void closeSession(SessionContext sctx) {
		server.closeSession(sctx);
	}
	
}
