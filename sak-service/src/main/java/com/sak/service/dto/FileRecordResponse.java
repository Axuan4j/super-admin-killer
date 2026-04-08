package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileRecordResponse {
    private Long id;
    private String bizType;
    private String fileName;
    private String storageName;
    private String filePath;
    private String downloadUrl;
    private String contentType;
    private String fileExt;
    private Long fileSize;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
}
