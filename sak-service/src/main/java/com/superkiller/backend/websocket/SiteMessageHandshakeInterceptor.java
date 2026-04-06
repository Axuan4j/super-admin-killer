package com.superkiller.backend.websocket;

import com.superkiller.backend.service.TokenService;
import com.superkiller.backend.utils.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class SiteMessageHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USERNAME_ATTR = "siteMessageUsername";
    public static final String SESSION_ID_ATTR = "siteMessageSessionId";

    private final JwtUtils jwtUtils;
    private final TokenService tokenService;

    public SiteMessageHandshakeInterceptor(JwtUtils jwtUtils, TokenService tokenService) {
        this.jwtUtils = jwtUtils;
        this.tokenService = tokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (!jwtUtils.validateToken(token) || !jwtUtils.isAccessToken(token) || !tokenService.isAccessTokenValid(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String sessionId = tokenService.getSessionIdByAccessToken(token);
        if (sessionId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(USERNAME_ATTR, jwtUtils.extractUsername(token));
        attributes.put(SESSION_ID_ATTR, sessionId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
