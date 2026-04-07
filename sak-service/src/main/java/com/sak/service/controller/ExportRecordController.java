package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.ExportRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/export-records")
@RequiredArgsConstructor
public class ExportRecordController {

    private final ExportRecordService exportRecordService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:export:view')")
    public Result<PageResponse<ExportRecordResponse>> listRecords(
            @RequestParam(name = "bizType", required = false) String bizType,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size
    ) {
        return Result.success(exportRecordService.listRecords(bizType, status, current, size));
    }
}
