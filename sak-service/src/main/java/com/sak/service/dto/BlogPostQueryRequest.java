package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BlogPostQueryRequest extends PageQuery {
    private String title;
    private String status;
    private Long categoryId;
}
