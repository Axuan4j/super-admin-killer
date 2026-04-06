package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItemResponse {
    private String dictType;
    private String label;
    private String value;
    private String tagType;
    private String tagColor;
    private Integer orderNum;
}
