package com.swingfrog.summer.server;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.swingfrog.summer.util.ThreadCountUtil;
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
	private final ServerContext serverContext;
	private final ServerPush serverPush;
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public Server(ServerConfig config, EventLoopGroup bossGroup, EventLoopGroup workerGroup, ExecutorService eventExecutor, ExecutorService pushExecutor) {
		serverContext = new ServerContext(config, new SessionHandlerGroup(), new SessionContextGroup(), eventExecutor, pushExecutor);
		serverPush = new ServerPush(serverContext);
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
	}

	public static Server create(ServerConfig config) {
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
		config.setUseMainServerThreadPool(false);
		return new Server(config,
				new NioEventLoopGroup(config.getBossThread(), new DefaultThreadFactory("ServerBoss")),
				new NioEventLoopGroup(config.getWorkerThread(), new DefaultThreadFactory("ServerWorker")),
				Executors.newFixedThreadPool(ThreadCountUtil.convert(config.getEventThread()), new DefaultThreadFactory("ServerEvent")),
				Executors.newSingleThreadExecutor(new DefaultThreadFactory("ServerPush")));
	}

	public static Server createMinor(ServerConfig config, EventLoopGroup bossGroup, EventLoopGroup workerGroup, ExecutorService eventExecutor, ExecutorService pushExecutor) {
		log.info("minor cluster {}", config.getCluster());
		log.info("minor serverName {}", config.getServerName());
		log.info("minor address {}", config.getAddress());
		log.info("minor port {}", config.getPort());
		log.info("minor protocol {}", config.getProtocol());
		log.info("minor charset {}", config.getCharset());
		log.info("minor password {}", config.getPassword());
		if (config.isUseMainServerThreadPool()) {
			log.info("minor use main server thread pool");
		} else {
			log.info("minor bossThread {}", config.getBossThread());
			log.info("minor workerThread {}", config.getWorkerThread());
			log.info("minor eventThread {}", config.getEventThread());
		}
		log.info("minor msgLength {}", config.getMsgLength());
		log.info("minor heartSec {}", config.getHeartSec());
		log.info("minor coldDownMs {}", config.getColdDownMs());
		log.info("minor allowAddressEnable {}", config.isAllowAddressEnable());
		log.info("minor allowAddressList {}", Arrays.toString(config.getAllowAddressList()));
		if (config.isUseMainServerThreadPool()) {
			return new Server(config, bossGroup, workerGroup, eventExecutor, pushExecutor);
		} else {
			return new Server(config,
					new NioEventLoopGroup(config.getBossThread(), new DefaultThreadFactory("ServerBoss_" + config.getServerName(), true)),
					new NioEventLoopGroup(config.getWorkerThread(), new DefaultThreadFactory("ServerWorker_" + config.getServerName(), true)),
					Executors.newFixedThreadPool(ThreadCountUtil.convert(config.getEventThread()), new DefaultThreadFactory("ServerEvent_" + config.getServerName())),
					Executors.newSingleThreadExecutor(new DefaultThreadFactory("ServerPush_" + config.getServerName())));
		}
	}

	public void launch() {
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 5)
					.childOption(ChannelOption.TCP_NODELAY, true);
			b.childHandler(new ServerInitializer(serverContext));
			b.bind(serverContext.getConfig().getAddress(), serverContext.getConfig().getPort()).sync();
			startCheckHeartTimeTask();
			log.info("server[{}] launch success", serverContext.getConfig().getServerName());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void shutdown() {
		log.info("server[{}] shutdown", serverContext.getConfig().getServerName());
		if (!serverContext.getConfig().isUseMainServerThreadPool()) {
			serverContext.getEventExecutor().shutdown();
			try {
				while (!serverContext.getEventExecutor().isTerminated()) {
					serverContext.getEventExecutor().awaitTermination(1, TimeUnit.SECONDS);
				}
			} catch (InterruptedException e){
				log.error(e.getMessage(), e);
			}
			serverContext.getPushExecutor().shutdown();
			try {
				while (!serverContext.getPushExecutor().isTerminated()) {
					serverContext.getPushExecutor().awaitTermination(1, TimeUnit.SECONDS);
				}
			} catch (InterruptedException e){
				log.error(e.getMessage(), e);
			}
			try {
				bossGroup.shutdownGracefully().sync();
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
			try {
				workerGroup.shutdownGracefully().sync();
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
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

	public ExecutorService getEventExecutor() {
		return serverContext.getEventExecutor();
	}

	public ExecutorService getPushExecutor() {
		return serverContext.getPushExecutor();
	}

	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}

	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}

	public ServerContext getServerContext() {
		return serverContext;
	}
	
}
