package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileShareLinkResponse {
    private Long fileId;
    private String fileName;
    private String shareUrl;
    private Boolean permanent;
    private Integer expireDays;
    private LocalDateTime expireAt;
}
