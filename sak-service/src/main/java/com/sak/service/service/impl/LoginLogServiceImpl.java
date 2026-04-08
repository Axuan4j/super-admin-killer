package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sak.service.dto.LoginLogQueryRequest;
import com.sak.service.dto.LoginLogResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.SysLoginLog;
import com.sak.service.mapper.SysLoginLogMapper;
import com.sak.service.service.IpRegionService;
import com.sak.service.service.LoginLogService;
import com.sak.service.util.PageUtils;
import com.sak.service.util.RequestIpUtils;
import com.sak.service.util.UserAgentUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private static final Map<String, String> LOGIN_LOG_SORT_FIELDS = new LinkedHashMap<>();

    static {
        LOGIN_LOG_SORT_FIELDS.put("id", "id");
        LOGIN_LOG_SORT_FIELDS.put("username", "username");
        LOGIN_LOG_SORT_FIELDS.put("loginIp", "login_ip");
        LOGIN_LOG_SORT_FIELDS.put("status", "status");
        LOGIN_LOG_SORT_FIELDS.put("loginTime", "login_time");
    }

    private final SysLoginLogMapper sysLoginLogMapper;
    private final IpRegionService ipRegionService;

    @Override
    public void recordSuccess(String username, HttpServletRequest request) {
        saveLoginLog(username, 1, "登录成功", request);
    }

    @Override
    public void recordFailure(String username, String message, HttpServletRequest request) {
        saveLoginLog(username, 0, truncateMessage(message, "登录失败"), request);
    }

    @Override
    public PageResponse<LoginLogResponse> listLogs(LoginLogQueryRequest request) {
        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getUsername())) {
            queryWrapper.like(SysLoginLog::getUsername, request.getUsername().trim());
        }
        if (StringUtils.hasText(request.getLoginIp())) {
            queryWrapper.like(SysLoginLog::getLoginIp, request.getLoginIp().trim());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(SysLoginLog::getStatus, request.getStatus());
        }
        Page<SysLoginLog> page = sysLoginLogMapper.selectPage(PageUtils.buildPage(request, LOGIN_LOG_SORT_FIELDS, "id", "desc"), queryWrapper);
        return PageUtils.toResponse(page, page.getRecords().stream().map(this::toResponse).toList(), request);
    }

    private void saveLoginLog(String username, Integer status, String message, HttpServletRequest request) {
        String loginIp = RequestIpUtils.resolveClientIp(request);
        String userAgent = request == null ? "" : defaultString(request.getHeader("User-Agent"));
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(StringUtils.hasText(username) ? username.trim() : "未知用户");
        loginLog.setLoginIp(loginIp);
        loginLog.setLoginLocation(ipRegionService.resolveRegion(loginIp));
        loginLog.setUserAgent(userAgent);
        loginLog.setBrowser(UserAgentUtils.resolveBrowser(userAgent));
        loginLog.setOs(UserAgentUtils.resolveOs(userAgent));
        loginLog.setStatus(status);
        loginLog.setMessage(truncateMessage(message, status != null && status == 1 ? "登录成功" : "登录失败"));
        loginLog.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(loginLog);
    }

    private LoginLogResponse toResponse(SysLoginLog loginLog) {
        LoginLogResponse response = new LoginLogResponse();
        response.setId(loginLog.getId());
        response.setUsername(loginLog.getUsername());
        response.setLoginIp(loginLog.getLoginIp());
        response.setLoginLocation(loginLog.getLoginLocation());
        response.setUserAgent(loginLog.getUserAgent());
        response.setBrowser(loginLog.getBrowser());
        response.setOs(loginLog.getOs());
        response.setStatus(loginLog.getStatus());
        response.setMessage(loginLog.getMessage());
        response.setLoginTime(loginLog.getLoginTime());
        return response;
    }

    private String truncateMessage(String message, String defaultMessage) {
        String value = StringUtils.hasText(message) ? message.trim() : defaultMessage;
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
