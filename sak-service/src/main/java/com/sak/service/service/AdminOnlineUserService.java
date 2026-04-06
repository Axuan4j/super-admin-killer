package com.sak.service.service;

import com.sak.service.dto.OnlineUserSessionResponse;

import java.util.List;

public interface AdminOnlineUserService {
    List<OnlineUserSessionResponse> listOnlineUsers(String currentSessionId);

    void forceLogout(String sessionId);
}
