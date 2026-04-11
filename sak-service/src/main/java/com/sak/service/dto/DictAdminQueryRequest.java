package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictAdminQueryRequest extends PageQuery {
    private String keyword;
    private String dictType;
    private String status;
}
