package com.superkiller.backend.service;

import com.superkiller.backend.entity.SysConfig;

import java.util.List;
import java.util.Map;

public interface SysConfigService {
    List<SysConfig> getAllConfigs();

    Map<String, String> getConfigMap();
}
