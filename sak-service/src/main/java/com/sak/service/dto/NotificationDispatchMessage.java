package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationDispatchMessage {
    private String title;
    private String content;
    private String senderName;
}
