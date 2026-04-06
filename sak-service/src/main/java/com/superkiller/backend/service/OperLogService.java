package com.superkiller.backend.service;

import com.superkiller.backend.dto.OperLogResponse;
import com.superkiller.backend.dto.PageResponse;

public interface OperLogService {
    PageResponse<OperLogResponse> listLogs(String keyword, Integer success, long current, long size);
}
