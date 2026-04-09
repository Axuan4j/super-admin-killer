package com.sak.service.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationChannelSendResult {

    private final boolean success;
    private final boolean skipped;
    private final String message;

    public static NotificationChannelSendResult success() {
        return new NotificationChannelSendResult(true, false, null);
    }

    public static NotificationChannelSendResult skipped(String message) {
        return new NotificationChannelSendResult(false, true, message);
    }

    public static NotificationChannelSendResult failed(String message) {
        return new NotificationChannelSendResult(false, false, message);
    }
}
