package com.sak.service.dto;

import com.sak.service.entity.SysSiteMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteMessageWsPayload {
    private String type;
    private Long unreadCount;
    private SysSiteMessage message;
    private String text;
}
