package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogResponse {
    private Long id;
    private String username;
    private String loginIp;
    private String loginLocation;
    private String userAgent;
    private String browser;
    private String os;
    private String deviceType;
    private Integer status;
    private String message;
    private LocalDateTime loginTime;
}
