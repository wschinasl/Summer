package com.swingfrog.summer.db.repository;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AsyncCacheRepositoryDao<T, K> extends CacheRepositoryDao<T, K> {

    private static final Logger log = LoggerFactory.getLogger(AsyncCacheRepositoryDao.class);

    private final ConcurrentLinkedQueue<Change<T>> waitChange = Queues.newConcurrentLinkedQueue();
    private final ConcurrentMap<T, Long> waitSave = Maps.newConcurrentMap();
    private long delayTime = delayTime();

    protected abstract long delayTime();

    @Override
    void init() {
        super.init();
        AsyncCacheRepositoryMgr.get().getScheduledExecutor().scheduleWithFixedDelay(
                () -> delay(false),
                delayTime,
                delayTime,
                TimeUnit.MILLISECONDS);
        AsyncCacheRepositoryMgr.get().addHook(() -> delay(true));
        if (delayTime >= expireTime()) {
            throw new RuntimeException(String.format("async cache repository delayTime[%s] must be less than expireTime[%s]", delayTime, expireTime()));
        }
    }

    private synchronized void delay(boolean force) {
        while (!waitChange.isEmpty()) {
            Change<T> change = waitChange.poll();
            if (change.add) {
                delayAdd(change.obj);
            } else {
                delayRemove(change.obj);
            }
        }
        delaySave(force);
    }

    private void delayAdd(T obj) {
        super.addNotAutoIncrement(obj);
    }

    private void delayRemove(T obj) {
        super.remove(obj);
    }

    private void delaySave(boolean force) {
        long time = System.currentTimeMillis();
        List<T> list = waitSave.entrySet().stream()
                .filter(entry -> force || time - entry.getValue() >= delayTime)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!list.isEmpty()) {
            super.save(list);
        }
        list.forEach(waitSave::remove);
    }

    @Override
    public boolean add(T obj) {
        super.autoIncrementPrimaryKey(obj);
        super.addCache(obj);
        waitChange.add(new Change<>(obj, true));
        return true;
    }

    @Override
    public boolean remove(T obj) {
        super.forceSetCacheEmpty(obj);
        waitChange.add(new Change<>(obj, false));
        return true;
    }

    @Override
    public boolean save(T obj) {
        Objects.requireNonNull(obj, "async cache repository save param not null");
        K pk = (K) TableValueBuilder.getPrimaryKeyValue(tableMeta, obj);
        T newObj = get(pk);
        if (obj != newObj) {
            log.warn("async cache repository table[{}] primary key[{}] expire, can't save", tableMeta.getName(), pk);
            return false;
        }
        long time = System.currentTimeMillis();
        waitSave.put(obj, time);
        return true;
    }

    @Override
    public void save(List<T> objs) {
        objs.forEach(this::save);
    }

    private static class Change<T> {
        T obj;
        boolean add;
        Change(T obj, boolean add) {
            this.obj = obj;
            this.add = add;
        }
    }

    public boolean syncAdd(T obj) {
        return super.add(obj);
    }

    public boolean syncRemove(T obj) {
        return super.remove(obj);
    }

    public boolean syncSave(T obj) {
        return super.save(obj);
    }

    public void syncSave(List<T> objs) {
        super.save(objs);
    }

}
