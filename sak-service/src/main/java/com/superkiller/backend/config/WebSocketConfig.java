package com.superkiller.backend.config;

import com.superkiller.backend.websocket.SiteMessageHandshakeInterceptor;
import com.superkiller.backend.websocket.SiteMessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SiteMessageWebSocketHandler siteMessageWebSocketHandler;
    private final SiteMessageHandshakeInterceptor siteMessageHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(siteMessageWebSocketHandler, "/ws/site-messages")
                .addInterceptors(siteMessageHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
