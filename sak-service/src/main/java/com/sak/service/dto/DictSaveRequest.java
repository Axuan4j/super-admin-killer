package com.sak.service.dto;

import lombok.Data;

@Data
public class DictSaveRequest {
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private String tagType;
    private String tagColor;
    private Integer orderNum;
    private String status;
    private String remark;
}
