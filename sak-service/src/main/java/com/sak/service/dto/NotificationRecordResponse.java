package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class NotificationRecordResponse {
    private Long id;
    private String senderName;
    private String title;
    private String content;
    private List<String> channels = new ArrayList<>();
    private Boolean sendAll;
    private Integer recipientCount;
    private Integer successUserCount;
    private String status;
    private Map<String, Integer> channelSuccessCounts = new LinkedHashMap<>();
    private Map<String, Integer> channelSkipCounts = new LinkedHashMap<>();
    private LocalDateTime createTime;
}
