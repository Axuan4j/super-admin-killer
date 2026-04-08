package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.ExportRecordQueryRequest;
import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.ExportRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/export-records")
@RequiredArgsConstructor
public class ExportRecordController {

    private final ExportRecordService exportRecordService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:export:view')")
    public Result<PageResponse<ExportRecordResponse>> listRecords(ExportRecordQueryRequest request) {
        return Result.success(exportRecordService.listRecords(request));
    }
}
