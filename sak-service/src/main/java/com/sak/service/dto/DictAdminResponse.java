package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictAdminResponse {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private String tagType;
    private String tagColor;
    private Integer orderNum;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
