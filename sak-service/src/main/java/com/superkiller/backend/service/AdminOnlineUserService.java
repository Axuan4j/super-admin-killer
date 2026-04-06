package com.superkiller.backend.service;

import com.superkiller.backend.dto.OnlineUserSessionResponse;

import java.util.List;

public interface AdminOnlineUserService {
    List<OnlineUserSessionResponse> listOnlineUsers(String currentSessionId);

    void forceLogout(String sessionId);
}
