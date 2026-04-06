package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping("/all")
    public Result<Map<String, String>> getAllConfigs() {
        return Result.success(sysConfigService.getConfigMap());
    }
}
