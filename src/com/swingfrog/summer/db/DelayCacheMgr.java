package com.swingfrog.summer.db;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.swingfrog.summer.config.ConfigUtil;
import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DelayCacheMgr {

    private static final Logger log = LoggerFactory.getLogger(DelayCacheMgr.class);

    private static ScheduledExecutorService scheduledExecutor;
    private DelayCacheConfig config = new DelayCacheConfig();
    private ConcurrentSet<Runnable> hooks = new ConcurrentSet<>();

    private static class SingleCase {
        public static final DelayCacheMgr INSTANCE = new DelayCacheMgr();
    }

    private DelayCacheMgr() {

    }

    public static DelayCacheMgr get() {
        return SingleCase.INSTANCE;
    }

    public void loadConfig(String path) throws IOException {
        Properties pro = new Properties();
        FileInputStream in = new FileInputStream(path);
        pro.load(in);
        ConfigUtil.loadDataWithBean(pro, "delayCache.", config);
        in.close();
        pro.clear();
        if (config.getCoreThread() <= 0) {
            config.setCoreThread(1);
        }
        log.info("delay cache mgr loading config, core thread num[{}]", config.getCoreThread());
    }

    public void shutdown() {
        if (scheduledExecutor != null) {
            log.info("delay cache mgr shutdown");
            scheduledExecutor.shutdown();
        }
        hooks.forEach(Runnable::run);
    }

    public ScheduledExecutorService getScheduledExecutor() {
        if (scheduledExecutor == null) {
            synchronized (this) {
                if (scheduledExecutor == null) {
                    scheduledExecutor = Executors.newScheduledThreadPool(
                            config.getCoreThread(),
                            new ThreadFactoryBuilder().setNameFormat("DelayCacheMgr-%s").build());
                    log.info("delay cache mgr create scheduled executor");
                }
            }
        }
        return scheduledExecutor;
    }

    public void addHook(Runnable hook) {
        hooks.add(hook);
    }
}
