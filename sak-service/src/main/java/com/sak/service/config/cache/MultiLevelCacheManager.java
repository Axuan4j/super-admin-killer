package com.sak.service.config.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiLevelCacheManager implements CacheManager {

    private final CacheManager localCacheManager;
    private final CacheManager redisCacheManager;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public MultiLevelCacheManager(CacheManager localCacheManager, CacheManager redisCacheManager) {
        this.localCacheManager = localCacheManager;
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        LinkedHashSet<String> cacheNames = new LinkedHashSet<>(localCacheManager.getCacheNames());
        cacheNames.addAll(redisCacheManager.getCacheNames());
        return cacheNames;
    }

    private Cache createCache(String name) {
        Cache localCache = localCacheManager.getCache(name);
        Cache redisCache = redisCacheManager.getCache(name);
        if (localCache == null || redisCache == null) {
            throw new IllegalStateException("Unable to create multilevel cache for " + name);
        }
        return new MultiLevelCache(name, localCache, redisCache);
    }
}
