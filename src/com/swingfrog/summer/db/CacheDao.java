package com.swingfrog.summer.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.swingfrog.summer.util.StringUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class CacheDao<T extends AbstractPersistenceEntity> extends PersistenceDao<T> {

    private static final String PREFIX = "CACHE_DAO";
    private final Cache<Long, T> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(expireTime(), TimeUnit.MILLISECONDS)
            .build();
    private final AtomicBoolean findAll = new AtomicBoolean(false);

    @Override
	public T add(T n) {
	    synchronized (n) {
            super.add(n);
            cache.put(n.getId(), n);
            return n;
        }
    }

    @Override
    public T get(long id) {
        T n = cache.getIfPresent(id);
        if (n == null) {
            synchronized (StringUtil.getString(PREFIX, getTableName(), "GET", id)) {
                n = cache.getIfPresent(id);
                if (n == null) {
                    n = super.get(id);
                    if (n != null) {
                        cache.put(n.getId(), n);
                    }
                }
            }
        }
	    return n;
    }

    @Override
    public List<T> list() {
        if (!findAll.get()) {
            synchronized (StringUtil.getString(PREFIX, getTableName(), "LIST")) {
                if (!findAll.get()) {
                    super.list().forEach(n -> {
                        if (cache.getIfPresent(n.getId()) == null)
                            cache.put(n.getId(), n);
                    });
                    findAll.set(true);
                }
            }
        }
	    return cache.asMap().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    @Override
    public boolean update(T n) {
	    synchronized (StringUtil.getString(PREFIX, getTableName(), "UPDATE", n.getId())) {
            if (super.update(n)) {
                cache.put(n.getId(), n);
                return true;
            }
        }
	    return false;
    }

    @Override
    public Map<T, Boolean> update(List<T> ns) {
        synchronized (StringUtil.getString(PREFIX, getTableName(), "SAVE_ALL")) {
            Map<T, Boolean> resultMap = super.update(ns);
            ns.forEach(n -> {
                if (resultMap.getOrDefault(n, false)) {
                    cache.put(n.getId(), n);
                }
            });
            return resultMap;
        }
    }

    @Override
    public boolean delete(T n) {
        synchronized (StringUtil.getString(PREFIX, getTableName(), "DELETE", n.getId())) {
            if (super.delete(n)) {
                cache.invalidate(n.getId());
                return true;
            }
        }
	    return false;
    }

    protected abstract long expireTime();
}
