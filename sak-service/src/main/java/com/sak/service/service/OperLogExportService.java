package com.sak.service.service;

import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.OperLogExportRequest;

public interface OperLogExportService {
    ExportRecordResponse createExportTask(OperLogExportRequest request, String operator);
}
