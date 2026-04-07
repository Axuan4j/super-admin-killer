package com.sak.service.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class NotificationSendResponse {
    private int recipientCount;
    private int successUserCount;
    private Map<String, Integer> channelSuccessCounts = new LinkedHashMap<>();
    private Map<String, Integer> channelSkipCounts = new LinkedHashMap<>();
}
