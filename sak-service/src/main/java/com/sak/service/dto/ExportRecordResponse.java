package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportRecordResponse {
    private Long id;
    private String bizType;
    private String fileName;
    private String queryCondition;
    private String status;
    private String filePath;
    private String downloadUrl;
    private Long fileSize;
    private Long totalCount;
    private String operator;
    private String errMsg;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
}
