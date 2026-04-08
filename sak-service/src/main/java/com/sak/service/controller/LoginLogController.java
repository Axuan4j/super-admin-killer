package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.LoginLogQueryRequest;
import com.sak.service.dto.LoginLogResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:login-log:view')")
    public Result<PageResponse<LoginLogResponse>> listLogs(LoginLogQueryRequest request) {
        return Result.success(loginLogService.listLogs(request));
    }
}
