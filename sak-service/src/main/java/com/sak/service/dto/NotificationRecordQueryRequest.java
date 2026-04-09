package com.sak.service.dto;

import lombok.Data;

@Data
public class NotificationRecordQueryRequest extends PageQuery {
    private String title;
    private String senderName;
    private String channelCode;
    private String status;
}
