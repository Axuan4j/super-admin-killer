package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.MonitorOverviewResponse;
import com.sak.service.service.SystemMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/monitor")
@RequiredArgsConstructor
public class SystemMonitorController {

    private final SystemMonitorService systemMonitorService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('system:monitor:view')")
    public Result<MonitorOverviewResponse> getOverview() {
        return Result.success(systemMonitorService.getOverview());
    }
}
