package com.sak.service.service;

import com.sak.service.dto.OperLogResponse;
import com.sak.service.dto.PageResponse;

public interface OperLogService {
    PageResponse<OperLogResponse> listLogs(String keyword, Integer success, long current, long size);
}
