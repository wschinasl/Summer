package com.swingfrog.summer.db.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.swingfrog.summer.util.StringUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public abstract class CacheRepositoryDao<T, K> extends RepositoryDao<T, K> {

    private static final String PREFIX = "CacheRepositoryDao";
    private T EMPTY;
    private final Cache<K, T> cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize())
            .expireAfterAccess(expireTime(), TimeUnit.MILLISECONDS)
            .build();
    private final Map<String, Cache<Object, Set<K>>> cachePkMap = Maps.newHashMap();
    private final Map<String, Cache<Object, Boolean>> cachePkFinishMap = Maps.newHashMap();
    private final AtomicLong findAllTime = new AtomicLong(0);
    private final long expireTime = expireTime();

    protected abstract long expireTime();
    protected abstract long maxSize();

    @Override
    void init() {
        super.init();
        try {
            EMPTY = getEntityClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("cache repository EMPTY not null");
        }
        tableMeta.getCacheKeys().forEach(columnMeta -> {
            cachePkMap.put(columnMeta.getName(),
                    CacheBuilder.newBuilder()
                            .expireAfterAccess(expireTime, TimeUnit.MILLISECONDS)
                            .build());
            cachePkFinishMap.put(columnMeta.getName(),
                    CacheBuilder.newBuilder()
                            .expireAfterAccess(expireTime, TimeUnit.MILLISECONDS)
                            .build());
        });
    }

    @Override
    protected boolean addNotAutoIncrement(T obj) {
        boolean ok = super.addNotAutoIncrement(obj);
        if (ok)
            addCache(obj);
        return ok;
    }

    @Override
    public boolean add(T obj) {
        boolean ok = super.add(obj);
        if (ok)
            addCache(obj);
        return ok;
    }

    @Override
    public boolean remove(T obj) {
        boolean ok = super.remove(obj);
        if (ok)
            removeCache(obj);
        return ok;
    }

    @Override
    public boolean save(T obj) {
        return super.save(obj);
    }

    @Override
    public void save(List<T> objs) {
        super.save(objs);
    }

    @Override
    public T get(K primaryKey) {
        T obj = cache.getIfPresent(primaryKey);
        if (obj == null) {
            synchronized (StringUtil.getString(PREFIX, tableMeta.getName(), "get", primaryKey)) {
                obj = cache.getIfPresent(primaryKey);
                if (obj == null) {
                    obj = super.get(primaryKey);
                    if (obj != null) {
                        addCache(primaryKey, obj);
                    } else {
                        addCache(primaryKey, EMPTY);
                    }
                }
            }
        } else if (obj == EMPTY) {
            return null;
        }
        return obj;
    }

    @Override
    public List<T> list(String field, Object value) {
        if (tableMeta.getCacheKeys().contains(tableMeta.getColumnMetaMap().get(field))) {
            return listPrimaryValueByCacheKey(field, value).stream().map(this::get).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return list(ImmutableMap.of(field, value));
    }

    @Override
    public List<T> list(Map<String, Object> optional) {
        LinkedList<List<K>> pkList = Lists.newLinkedList();
        Map<String, Object> normal = Maps.newHashMap();
        optional.entrySet().forEach(entry -> {
            if (tableMeta.getCacheKeys().contains(tableMeta.getColumnMetaMap().get(entry.getKey()))) {
                pkList.add(listPrimaryValueByCacheKey(entry.getKey(), entry.getValue()));
            } else {
                normal.put(entry.getKey(), entry.getValue());
            }
        });
        List<T> list;
        if (pkList.isEmpty()) {
            super.listPrimaryKey(optional).forEach(this::get);
            list = cache.asMap().values().stream()
                    .filter(obj -> obj != EMPTY)
                    .sorted(Comparator.comparingInt(Objects::hashCode))
                    .collect(Collectors.toList());
        } else {
            if (pkList.size() == 1) {
                list = pkList.get(0).stream().map(this::get).filter(Objects::nonNull).collect(Collectors.toList());
            } else {
                List<K> first = pkList.removeFirst();
                for (Iterator<K> iterator = first.iterator(); iterator.hasNext(); ) {
                    Object obj = iterator.next();
                    for (List<K> pk : pkList) {
                        if (!pk.contains(obj)) {
                            iterator.remove();
                            break;
                        }
                    }
                }
                list = first.stream().map(this::get).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }
        if (normal.size() > 0) {
            list = list.stream().filter(obj -> {
                for (Map.Entry<String, Object> entry : normal.entrySet()) {
                    if (!TableValueBuilder.isEqulsColumnValue(tableMeta.getColumnMetaMap().get(entry.getKey()), obj, entry.getValue())) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
        }
        return list;
    }

    @Override
    public List<T> list() {
        long time = System.currentTimeMillis();
        if (time - expireTime() >= findAllTime.get()) {
            synchronized (StringUtil.getString(PREFIX, tableMeta.getName(), "list")) {
                if (time - expireTime() >= findAllTime.get()) {
                    listPrimaryKey().forEach(this::get);
                }
            }
        }
        findAllTime.set(time);
        return cache.asMap().keySet().stream()
                .map(this::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(obj -> TableValueBuilder.getPrimaryKeyValue(tableMeta, obj).hashCode()))
                .collect(Collectors.toList());
    }

    protected List<K> listPrimaryValueByCacheKey(String column, Object cacheValue) {
        Cache<Object, Set<K>> cachePk = cachePkMap.get(column);
        Set<K> pkSet = cachePk.getIfPresent(cacheValue);
        if (cachePkFinishMap.get(column).getIfPresent(cacheValue) == null || pkSet == null) {
            synchronized (StringUtil.getString(PREFIX, tableMeta.getName(), "CacheKey", column, cacheValue)) {
                pkSet = cachePk.getIfPresent(cacheValue);
                if (pkSet == null) {
                    pkSet = Sets.newConcurrentHashSet();
                    cachePk.put(cacheValue, pkSet);
                }
            }
            cachePkFinishMap.get(column).put(cacheValue, true);
            pkSet.addAll(listPrimaryKey(ImmutableMap.of(column, cacheValue)));
        }
        return pkSet.stream().sorted(Comparator.comparingInt(Objects::hashCode)).collect(Collectors.toList());
    }

    protected void addCache(T obj) {
        addCache((K) TableValueBuilder.getPrimaryKeyValue(tableMeta, obj), obj);
    }

    protected void forceSetCacheEmpty(T obj) {
        K primaryKey = (K) TableValueBuilder.getPrimaryKeyValue(tableMeta, obj);
        synchronized (StringUtil.getString(PREFIX, tableMeta.getName(), "addCache", primaryKey)) {
            cache.put(primaryKey, EMPTY);
        }
    }

    protected void addCache(K primaryKey, T obj) {
        synchronized (StringUtil.getString(PREFIX, tableMeta.getName(), "addCache", primaryKey)) {
            T old = cache.getIfPresent(primaryKey);
            if (old == null || old == EMPTY) {
                cache.put(primaryKey, obj);
            }
            if (obj == EMPTY) {
                return;
            }
            tableMeta.getCacheKeys().forEach(columnMeta -> {
                Object cacheValue = TableValueBuilder.getColumnValue(columnMeta, obj);
                Cache<Object, Set<K>> cachePk = cachePkMap.get(columnMeta.getName());
                Set<K> pkSet = cachePk.getIfPresent(cacheValue);
                cachePkFinishMap.get(columnMeta.getName()).getIfPresent(cacheValue);
                if (pkSet == null) {
                    synchronized (StringUtil.getString(PREFIX, tableMeta.getName(), "CacheKey", columnMeta.getName(), cacheValue)) {
                        pkSet = cachePk.getIfPresent(cacheValue);
                        if (pkSet == null) {
                            pkSet = Sets.newConcurrentHashSet();
                            cachePk.put(cacheValue, pkSet);
                        }
                    }
                }
                pkSet.add(primaryKey);
            });
        }
    }

    protected void removeCache(T obj) {
        K primaryKey = (K) TableValueBuilder.getPrimaryKeyValue(tableMeta, obj);
        cache.invalidate(primaryKey);
        tableMeta.getCacheKeys().forEach(columnMeta -> {
            cachePkMap.get(columnMeta.getName()).invalidateAll();
            cachePkFinishMap.get(columnMeta.getName()).invalidateAll();
        });
    }

}
