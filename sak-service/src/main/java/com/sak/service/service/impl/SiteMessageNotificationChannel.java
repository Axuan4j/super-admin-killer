package com.sak.service.service.impl;

import com.sak.service.dto.NotificationChannelSendResult;
import com.sak.service.dto.NotificationDispatchMessage;
import com.sak.service.entity.SysUser;
import com.sak.service.service.NotificationChannel;
import com.sak.service.service.SysSiteMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteMessageNotificationChannel implements NotificationChannel {

    public static final String CHANNEL_CODE = "SITE_MESSAGE";

    private final SysSiteMessageService sysSiteMessageService;

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public void validateGlobalConfig() {
        // 站内信无需额外全局配置
    }

    @Override
    public NotificationChannelSendResult send(NotificationDispatchMessage message, SysUser recipient) {
        sysSiteMessageService.sendMessageToUser(recipient.getId(), message.getTitle(), message.getContent(), message.getSenderName());
        return NotificationChannelSendResult.success();
    }
}
