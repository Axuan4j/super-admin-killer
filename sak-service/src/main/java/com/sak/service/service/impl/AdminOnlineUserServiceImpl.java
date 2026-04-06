package com.sak.service.service.impl;

import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.OnlineUserSessionResponse;
import com.sak.service.service.AdminOnlineUserService;
import com.sak.service.service.SiteMessagePushService;
import com.sak.service.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminOnlineUserServiceImpl implements AdminOnlineUserService {

    private final TokenService tokenService;
    private final SiteMessagePushService siteMessagePushService;

    @Override
    public List<OnlineUserSessionResponse> listOnlineUsers(String currentSessionId) {
        return tokenService.listOnlineSessions().stream()
                .peek(session -> session.setCurrent(Objects.equals(currentSessionId, session.getSessionId())))
                .toList();
    }

    @Override
    @LogRecord(success = "强制下线会话：{{#sessionId}}", fail = "强制下线失败：{{#sessionId}}", type = "ONLINE", subType = "FORCE_LOGOUT", bizNo = "{{#sessionId}}")
    public void forceLogout(String sessionId) {
        if (!tokenService.hasSession(sessionId)) {
            throw new IllegalArgumentException("在线会话不存在或已失效");
        }
        siteMessagePushService.pushForceLogout(sessionId, "当前会话已被管理员强制下线");
        tokenService.deleteSession(sessionId);
    }
}
