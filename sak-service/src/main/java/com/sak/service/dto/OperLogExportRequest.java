package com.sak.service.dto;

import lombok.Data;

@Data
public class OperLogExportRequest {
    private String operator;
    private String logType;
    private String action;
    private Integer success;
}
