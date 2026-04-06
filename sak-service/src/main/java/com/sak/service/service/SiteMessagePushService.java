package com.sak.service.service;

import com.sak.service.entity.SysSiteMessage;

public interface SiteMessagePushService {
    void pushUnreadCount(String username, long unreadCount);

    void pushNewMessage(String username, long unreadCount, SysSiteMessage message);

    void pushForceLogout(String sessionId, String text);
}
