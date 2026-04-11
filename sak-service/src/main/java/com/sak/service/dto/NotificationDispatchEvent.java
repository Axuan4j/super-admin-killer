package com.sak.service.dto;

import com.sak.service.entity.SysUser;

import java.util.List;

public record NotificationDispatchEvent(
        Long recordId,
        NotificationDispatchMessage message,
        List<SysUser> recipients,
        List<String> channels, boolean sendAll) {
}
