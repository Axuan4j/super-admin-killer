package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileRecordQueryRequest extends PageQuery {
    private String bizType;
    private String keyword;
    private String operator;
}
