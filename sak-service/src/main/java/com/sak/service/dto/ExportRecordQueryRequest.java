package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExportRecordQueryRequest extends PageQuery {
    private String bizType;
    private String status;
}
