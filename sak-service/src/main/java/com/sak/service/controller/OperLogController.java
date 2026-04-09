package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.OperLogQueryRequest;
import com.sak.service.dto.OperLogResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.convert.RequestVoConverter;
import com.sak.service.service.OperLogExportService;
import com.sak.service.service.OperLogService;
import com.sak.service.vo.OperLogExportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/system/logs")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;
    private final OperLogExportService operLogExportService;
    private final RequestVoConverter requestVoMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('system:log:view')")
    public Result<PageResponse<OperLogResponse>> listLogs(OperLogQueryRequest request) {
        return Result.success(operLogService.listLogs(request));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('system:log:export')")
    public Result<ExportRecordResponse> exportLogs(Authentication authentication, @RequestBody(required = false) OperLogExportVO request) {
        var exportRequest = request == null ? requestVoMapper.toOperLogExportRequest(new OperLogExportVO()) : requestVoMapper.toOperLogExportRequest(request);
        return Result.success(operLogExportService.createExportTask(exportRequest, authentication == null ? "系统" : authentication.getName()));
    }
}
