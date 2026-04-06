package com.superkiller.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class NotificationSendRequest {
    private Boolean sendAll;
    private List<Long> userIds;
    private String title;
    private String content;
}
