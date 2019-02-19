package com.swingfrog.summer.server;

import java.util.Arrays;
import java.util.Iterator;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.config.ServerConfig;
import com.swingfrog.summer.task.TaskMgr;
import com.swingfrog.summer.task.TaskUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);
	private ServerContext serverContext;
	private ServerPush serverPush;

	public Server(ServerConfig config) {
		log.info("server cluster {}", config.getCluster());
		log.info("server serverName {}", config.getServerName());
		log.info("server address {}", config.getAddress());
		log.info("server port {}", config.getPort());
		log.info("server protocol {}", config.getProtocol());
		log.info("server charset {}", config.getCharset());
		log.info("server password {}", config.getPassword());
		log.info("server bossThread {}", config.getBossThread());
		log.info("server workerThread {}", config.getWorkerThread());
		log.info("server eventThread {}", config.getEventThread());
		log.info("server msgLength {}", config.getMsgLength());
		log.info("server heartSec {}", config.getHeartSec());
		log.info("server coldDownMs {}", config.getColdDownMs());
		log.info("server allowAddressEnable {}", config.isAllowAddressEnable());
		log.info("server allowAddressList {}", Arrays.toString(config.getAllowAddressList()));
		serverContext = new ServerContext(config, new SessionHandlerGroup(), new SessionContextGroup(),
				new NioEventLoopGroup(config.getEventThread(), new DefaultThreadFactory("ServerEvent", true)));
		serverPush = new ServerPush(serverContext);
	}

	public void launch() {
		EventLoopGroup bossGroup = new NioEventLoopGroup(serverContext.getConfig().getBossThread(), new DefaultThreadFactory("ServerBoss", true));
		EventLoopGroup workerGroup = new NioEventLoopGroup(serverContext.getConfig().getWorkerThread(), new DefaultThreadFactory("ServerWorker", true));
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG,1024);
			b.childHandler(new ServerInitializer(serverContext));
			b.bind(serverContext.getConfig().getAddress(), serverContext.getConfig().getPort()).sync();
			startCheckHeartTimeTask();
			log.info("server launch success");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void addSessionHandler(SessionHandler sessionHandler) {
		serverContext.getSessionHandlerGroup().addSessionHandler(sessionHandler);
	}

	public ServerPush getServerPush() {
		return serverPush;
	}
	
	public void closeSession(SessionContext sctx) {
		serverContext.getSessionContextGroup().getChannelBySession(sctx).close();
	}

	private void startCheckHeartTimeTask() throws SchedulerException {
		int interval = serverContext.getConfig().getHeartSec() / 2;
		TaskMgr.get().start(TaskUtil.getIntervalTask(interval * 1000, interval * 1000, serverContext.getConfig().getServerName(), () -> {
			log.info("check all client connect");
			Iterator<SessionContext> ite = serverContext.getSessionContextGroup().iteratorSession();
			while (ite.hasNext()) {
				SessionContext ctx = ite.next();
				ctx.setHeartCount(ctx.getHeartCount() + interval);
				if (ctx.getHeartCount() >= serverContext.getConfig().getHeartSec()) {
					serverContext.getSessionHandlerGroup().heartTimeOut(ctx);
				}
			}
		}));
	}

	public EventLoopGroup getEventLoopGroup() {
		return serverContext.getEventGroup();
	}
	
}
