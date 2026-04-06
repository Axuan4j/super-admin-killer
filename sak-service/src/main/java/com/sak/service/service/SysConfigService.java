package com.sak.service.service;

import com.sak.service.entity.SysConfig;

import java.util.List;
import java.util.Map;

public interface SysConfigService {
    List<SysConfig> getAllConfigs();

    Map<String, String> getConfigMap();
}
