package com.sak.service.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.config.NotificationProperties;
import com.sak.service.dto.NotificationChannelSendResult;
import com.sak.service.dto.NotificationDispatchMessage;
import com.sak.service.entity.SysUser;
import com.sak.service.service.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WxPusherNotificationChannel implements NotificationChannel {

    public static final String CHANNEL_CODE = "WXPUSHER";

    private final NotificationProperties notificationProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public void validateGlobalConfig() {
        NotificationProperties.Wxpusher properties = notificationProperties.getWxpusher();
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("WxPusher 通道未启用，请先配置 notification.wxpusher.enabled=true");
        }
        if (!StringUtils.hasText(properties.getAppToken())) {
            throw new IllegalArgumentException("WxPusher 通道未配置 appToken");
        }
    }

    @Override
    public NotificationChannelSendResult send(NotificationDispatchMessage message, SysUser recipient) {
        if (!StringUtils.hasText(recipient.getWxPusherUid())) {
            return NotificationChannelSendResult.skipped("用户未配置 WxPusher UID");
        }

        NotificationProperties.Wxpusher properties = notificationProperties.getWxpusher();
        String requestBody = buildRequestBody(message, recipient.getWxPusherUid(), properties);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl().replaceAll("/+$", "") + "/api/send/message"))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("WxPusher 请求失败，HTTP " + response.statusCode());
            }

            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object code = result.get("code");
            if (!(code instanceof Number) || ((Number) code).intValue() != 1000) {
                throw new IllegalStateException("WxPusher 返回异常：" + String.valueOf(result.get("msg")));
            }
            return NotificationChannelSendResult.success();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("WxPusher 推送失败: " + ex.getMessage(), ex);
        }
    }

    private String buildRequestBody(NotificationDispatchMessage message, String uid, NotificationProperties.Wxpusher properties) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "appToken", properties.getAppToken(),
                    "content", message.getContent(),
                    "summary", message.getTitle(),
                    "contentType", properties.getContentType(),
                    "uids", List.of(uid.trim())
            ));
        } catch (IOException ex) {
            throw new IllegalStateException("构造 WxPusher 请求失败", ex);
        }
    }
}
