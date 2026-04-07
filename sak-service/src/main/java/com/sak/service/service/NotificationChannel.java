package com.sak.service.service;

import com.sak.service.dto.NotificationChannelSendResult;
import com.sak.service.dto.NotificationDispatchMessage;
import com.sak.service.entity.SysUser;

public interface NotificationChannel {

    String getChannelCode();

    void validateGlobalConfig();

    NotificationChannelSendResult send(NotificationDispatchMessage message, SysUser recipient);
}
