package com.sak.service.websocket;

import com.sak.service.service.SysSiteMessageService;
import com.sak.service.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteMessageWebSocketHandler extends TextWebSocketHandler {

    private final SiteMessageSessionRegistry sessionRegistry;
    private final SysSiteMessageService sysSiteMessageService;
    private final TokenService tokenService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = getUsername(session);
        String sessionId = getBusinessSessionId(session);
        if (username == null || sessionId == null) {
            return;
        }
        sessionRegistry.register(username, sessionId, session);
        tokenService.touchSessionBySessionId(sessionId);
        sysSiteMessageService.syncUnreadCount(username);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("PING".equalsIgnoreCase(message.getPayload())) {
            String sessionId = getBusinessSessionId(session);
            if (sessionId != null) {
                tokenService.touchSessionBySessionId(sessionId);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug("Site message websocket transport error", exception);
        cleanup(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanup(session);
    }

    private void cleanup(WebSocketSession session) {
        sessionRegistry.unregister(session);
    }

    private String getUsername(WebSocketSession session) {
        Object username = session.getAttributes().get(SiteMessageHandshakeInterceptor.USERNAME_ATTR);
        return username instanceof String ? (String) username : null;
    }

    private String getBusinessSessionId(WebSocketSession session) {
        Object sessionId = session.getAttributes().get(SiteMessageHandshakeInterceptor.SESSION_ID_ATTR);
        return sessionId instanceof String ? (String) sessionId : null;
    }
}
