package com.superkiller.backend.config.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cache.multilevel")
public class MultiLevelCacheProperties {

    private Duration localExpireAfterWrite = Duration.ofMinutes(5);

    private Duration redisTimeToLive = Duration.ofMinutes(30);

    private long localMaximumSize = 1_000;

    private String redisKeyPrefix = "sak:cache:";

    public Duration getLocalExpireAfterWrite() {
        return localExpireAfterWrite;
    }

    public void setLocalExpireAfterWrite(Duration localExpireAfterWrite) {
        this.localExpireAfterWrite = localExpireAfterWrite;
    }

    public Duration getRedisTimeToLive() {
        return redisTimeToLive;
    }

    public void setRedisTimeToLive(Duration redisTimeToLive) {
        this.redisTimeToLive = redisTimeToLive;
    }

    public long getLocalMaximumSize() {
        return localMaximumSize;
    }

    public void setLocalMaximumSize(long localMaximumSize) {
        this.localMaximumSize = localMaximumSize;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }
}
