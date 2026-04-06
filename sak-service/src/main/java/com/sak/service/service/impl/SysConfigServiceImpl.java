package com.sak.service.service.impl;

import com.sak.service.entity.SysConfig;
import com.sak.service.mapper.SysConfigMapper;
import com.sak.service.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CONFIG_CACHE_KEY = "sys:config:all";
    private static final long CACHE_EXPIRE = 30; // 30分钟

    @Override
    public List<SysConfig> getAllConfigs() {
        @SuppressWarnings("unchecked")
        List<SysConfig> cached = (List<SysConfig>) redisTemplate.opsForValue().get(CONFIG_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        List<SysConfig> configs = sysConfigMapper.selectList(null);
        redisTemplate.opsForValue().set(CONFIG_CACHE_KEY, configs, CACHE_EXPIRE, TimeUnit.MINUTES);
        return configs;
    }

    @Override
    public Map<String, String> getConfigMap() {
        List<SysConfig> configs = getAllConfigs();
        return configs.stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
    }
}
