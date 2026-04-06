package com.sak.service.config.cache;

import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

public class MultiLevelCache implements Cache {

    private final String name;
    private final Cache localCache;
    private final Cache redisCache;

    public MultiLevelCache(String name, Cache localCache, Cache redisCache) {
        this.name = name;
        this.localCache = localCache;
        this.redisCache = redisCache;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        ValueWrapper localValue = localCache.get(key);
        if (localValue != null) {
            return localValue;
        }

        ValueWrapper redisValue = redisCache.get(key);
        if (redisValue != null) {
            localCache.put(key, redisValue.get());
        }
        return redisValue;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        T localValue = localCache.get(key, type);
        if (localValue != null) {
            return localValue;
        }

        T redisValue = redisCache.get(key, type);
        if (redisValue != null) {
            localCache.put(key, redisValue);
        }
        return redisValue;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        ValueWrapper localValue = localCache.get(key);
        if (localValue != null) {
            @SuppressWarnings("unchecked")
            T value = (T) localValue.get();
            return value;
        }

        ValueWrapper redisValue = redisCache.get(key);
        if (redisValue != null) {
            @SuppressWarnings("unchecked")
            T value = (T) redisValue.get();
            localCache.put(key, value);
            return value;
        }

        try {
            T loadedValue = valueLoader.call();
            if (loadedValue != null) {
                put(key, loadedValue);
            }
            return loadedValue;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        localCache.put(key, value);
        redisCache.put(key, value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(@NonNull Object key, @Nullable Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(@NonNull Object key) {
        localCache.evict(key);
        redisCache.evict(key);
    }

    @Override
    public boolean evictIfPresent(@NonNull Object key) {
        boolean localEvicted = localCache.evictIfPresent(key);
        boolean redisEvicted = redisCache.evictIfPresent(key);
        return localEvicted || redisEvicted;
    }

    @Override
    public void clear() {
        localCache.clear();
        redisCache.clear();
    }

    @Override
    public boolean invalidate() {
        boolean localInvalidated = localCache.invalidate();
        boolean redisInvalidated = redisCache.invalidate();
        return localInvalidated || redisInvalidated;
    }
}
