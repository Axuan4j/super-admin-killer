package com.sak.service.vo;

import lombok.Data;

@Data
public class OperLogExportVO {

    private String operator;

    private String logType;

    private String action;

    private Integer success;
}
