package com.superkiller.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SiteMessageSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> sessionsByBusinessSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> usernamesByWebSocketId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> businessSessionIdsByWebSocketId = new ConcurrentHashMap<>();

    public void register(String username, String businessSessionId, WebSocketSession session) {
        userSessions.computeIfAbsent(username, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionsByBusinessSessionId.put(businessSessionId, session);
        usernamesByWebSocketId.put(session.getId(), username);
        businessSessionIdsByWebSocketId.put(session.getId(), businessSessionId);
    }

    public void unregister(WebSocketSession session) {
        String username = usernamesByWebSocketId.remove(session.getId());
        String businessSessionId = businessSessionIdsByWebSocketId.remove(session.getId());
        if (businessSessionId != null) {
            sessionsByBusinessSessionId.remove(businessSessionId);
        }
        if (username == null) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(username);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(username);
        }
    }

    public Set<WebSocketSession> getSessions(String username) {
        return userSessions.getOrDefault(username, Collections.emptySet());
    }

    public WebSocketSession getSession(String businessSessionId) {
        return sessionsByBusinessSessionId.get(businessSessionId);
    }
}
