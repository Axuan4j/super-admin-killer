package com.superkiller.backend.dto;

import com.superkiller.backend.entity.SysSiteMessage;
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
