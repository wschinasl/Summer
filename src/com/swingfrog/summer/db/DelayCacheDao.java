package com.swingfrog.summer.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DelayCacheDao<T extends AbstractDelayCacheEntity> extends CacheDao<T> {

    private final AtomicBoolean lock = new AtomicBoolean(false);
    private final Set<T> waitSave = ConcurrentHashMap.newKeySet();

    public DelayCacheDao() {
        DelayCacheMgr.get().getScheduledExecutor().scheduleWithFixedDelay(
                () -> delayUpdate(false),
                intervalTime(),
                intervalTime(),
                TimeUnit.MILLISECONDS);
        DelayCacheMgr.get().addHook(() -> delayUpdate(true));
    }

    private void delayUpdate(boolean force) {
        if (!lock.compareAndSet(false, true)) {
            return;
        }
        List<T> list = new ArrayList<>(waitSave.size());
        long now = System.currentTimeMillis();
        waitSave.forEach(n -> {
            if (n.getLastModifyTime() != null) {
                if (force || now - n.getLastModifyTime() >= delayUpdateTime()) {
                    n.setLastModifyTime(null);
                    list.add(n);
                }
            }
        });
        if (!list.isEmpty()) {
            super.update(list);
        }
        lock.set(false);
    }

    @Override
    public boolean update(T n) {
        n.setLastModifyTime(System.currentTimeMillis());
        waitSave.add(n);
        return true;
    }

    @Override
    public Map<T, Boolean> update(List<T> ns) {
        long lastModifyTime = System.currentTimeMillis();
        Map<T, Boolean> resultMap = new HashMap<>();
        ns.forEach(n -> {
            n.setLastModifyTime(lastModifyTime);
            resultMap.put(n, true);
        });
        waitSave.addAll(ns);
        return resultMap;
    }

    protected abstract long delayUpdateTime();
    protected abstract long intervalTime();
}
