package com.sak.service.dto;

import lombok.Data;

@Data
public class OnlineUserSessionResponse {
    private String sessionId;
    private Long userId;
    private String username;
    private String nickName;
    private String ip;
    private String userAgent;
    private String loginTime;
    private String lastActiveTime;
    private boolean current;
}
