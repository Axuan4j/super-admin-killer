package com.superkiller.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superkiller.backend.dto.SiteMessageWsPayload;
import com.superkiller.backend.entity.SysSiteMessage;
import com.superkiller.backend.service.SiteMessagePushService;
import com.superkiller.backend.websocket.SiteMessageSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteMessagePushServiceImpl implements SiteMessagePushService {

    private final SiteMessageSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void pushUnreadCount(String username, long unreadCount) {
        broadcastToUser(username, new SiteMessageWsPayload("UNREAD_SYNC", unreadCount, null, null));
    }

    @Override
    public void pushNewMessage(String username, long unreadCount, SysSiteMessage message) {
        broadcastToUser(username, new SiteMessageWsPayload("NEW_MESSAGE", unreadCount, message, null));
    }

    @Override
    public void pushForceLogout(String sessionId, String text) {
        WebSocketSession session = sessionRegistry.getSession(sessionId);
        if (session == null) {
            return;
        }
        sendPayload(session, new SiteMessageWsPayload("FORCE_LOGOUT", 0L, null, text), null);
    }

    private void broadcastToUser(String username, SiteMessageWsPayload payload) {
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(username);
        if (sessions.isEmpty()) {
            return;
        }

        for (WebSocketSession session : sessions) {
            sendPayload(session, payload, username);
        }
    }

    private void sendPayload(WebSocketSession session, SiteMessageWsPayload payload, String username) {
        if (!session.isOpen()) {
            sessionRegistry.unregister(session);
            return;
        }

        String content = toJson(payload);
        TextMessage textMessage = new TextMessage(content);
        try {
            synchronized (session) {
                session.sendMessage(textMessage);
            }
        } catch (IOException e) {
            log.warn("Failed to push websocket payload to {}", username == null ? session.getId() : username, e);
            sessionRegistry.unregister(session);
            try {
                session.close();
            } catch (IOException closeException) {
                log.debug("Failed to close broken websocket session", closeException);
            }
        }
    }

    private String toJson(SiteMessageWsPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize websocket payload", e);
        }
    }
}
