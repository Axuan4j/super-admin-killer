package com.sak.service.service;

import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.SysExportRecord;

public interface ExportRecordService {
    SysExportRecord createPendingRecord(String bizType, String fileName, String queryCondition, String operator);

    void markRunning(Long id);

    void markSuccess(Long id, String filePath, long fileSize, long totalCount);

    void markFailed(Long id, String errMsg);

    PageResponse<ExportRecordResponse> listRecords(String bizType, String status, long current, long size);
}
