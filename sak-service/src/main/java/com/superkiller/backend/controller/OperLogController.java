package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.dto.OperLogResponse;
import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.service.OperLogService;
import com.superkiller.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/logs")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<PageResponse<OperLogResponse>> listLogs(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer success,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        permissionService.requirePermission(authentication, "system:log:view");
        return Result.success(operLogService.listLogs(keyword, success, current, size));
    }
}
