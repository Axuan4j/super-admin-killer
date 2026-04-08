package com.sak.service.service;

import com.sak.service.dto.LoginLogQueryRequest;
import com.sak.service.dto.LoginLogResponse;
import com.sak.service.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface LoginLogService {
    void recordSuccess(String username, HttpServletRequest request);

    void recordFailure(String username, String message, HttpServletRequest request);

    PageResponse<LoginLogResponse> listLogs(LoginLogQueryRequest request);
}
