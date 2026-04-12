package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogCategoryResponse {
    private Long id;
    private String categoryName;
    private String slug;
    private String description;
    private String coverImage;
    private Integer orderNum;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
