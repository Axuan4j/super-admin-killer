package com.sak.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class NotificationSendRequest {
    private Boolean sendAll;
    private List<Long> userIds;
    private List<String> channels;
    private String title;
    private String content;
}
