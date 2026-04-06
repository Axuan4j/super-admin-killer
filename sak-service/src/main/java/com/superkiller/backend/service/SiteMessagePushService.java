package com.superkiller.backend.service;

import com.superkiller.backend.entity.SysSiteMessage;

public interface SiteMessagePushService {
    void pushUnreadCount(String username, long unreadCount);

    void pushNewMessage(String username, long unreadCount, SysSiteMessage message);

    void pushForceLogout(String sessionId, String text);
}
