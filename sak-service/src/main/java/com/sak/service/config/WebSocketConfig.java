package com.sak.service.config;

import com.sak.service.websocket.SiteMessageHandshakeInterceptor;
import com.sak.service.websocket.SiteMessageWebSocketHandler;
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
