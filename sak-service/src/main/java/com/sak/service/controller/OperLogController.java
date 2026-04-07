package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.OperLogExportRequest;
import com.sak.service.dto.OperLogResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.OperLogExportService;
import com.sak.service.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/system/logs")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;
    private final OperLogExportService operLogExportService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:log:view')")
    public Result<PageResponse<OperLogResponse>> listLogs(
            @RequestParam(name = "operator", required = false) String operator,
            @RequestParam(name = "logType", required = false) String logType,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "success", required = false) Integer success,
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size
    ) {
        return Result.success(operLogService.listLogs(operator, logType, action, success, current, size));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('system:log:export')")
    public Result<ExportRecordResponse> exportLogs(Authentication authentication, @RequestBody(required = false) OperLogExportRequest request) {
        OperLogExportRequest exportRequest = request == null ? new OperLogExportRequest() : request;
        return Result.success(operLogExportService.createExportTask(exportRequest, authentication == null ? "系统" : authentication.getName()));
    }
}
