package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.service.SysConfigService;
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
