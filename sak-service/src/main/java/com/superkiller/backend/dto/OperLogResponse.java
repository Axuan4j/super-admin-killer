package com.superkiller.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperLogResponse {
    private Long id;
    private String bizNo;
    private String logType;
    private String subType;
    private String operator;
    private String action;
    private String extra;
    private String ip;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private Integer executionTime;
    private Integer success;
    private String errMsg;
    private LocalDateTime createTime;
}
