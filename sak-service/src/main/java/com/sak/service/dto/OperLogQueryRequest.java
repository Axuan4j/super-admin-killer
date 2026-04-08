package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQueryRequest extends PageQuery {
    private String operator;
    private String logType;
    private String action;
    private Integer success;
}
