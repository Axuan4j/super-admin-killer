package com.superkiller.backend.service;

import com.superkiller.backend.entity.SysSiteMessage;

import java.util.List;

public interface SysSiteMessageService {
    List<SysSiteMessage> getCurrentUserMessages(String username);

    long getCurrentUserUnreadCount(String username);

    int markCurrentUserMessagesRead(String username);

    SysSiteMessage createMessage(Long userId, String title, String content, String senderName);

    void sendMessageToUser(Long userId, String title, String content, String senderName);

    void syncUnreadCount(String username);
}
