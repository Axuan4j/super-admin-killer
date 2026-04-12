package com.sak.service.dto;

import lombok.Data;

@Data
public class BlogCategorySaveRequest {
    private String categoryName;
    private String slug;
    private String description;
    private String coverImage;
    private Integer orderNum;
    private String status;
    private String remark;
}
