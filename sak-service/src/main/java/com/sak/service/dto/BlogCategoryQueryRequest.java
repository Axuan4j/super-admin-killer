package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BlogCategoryQueryRequest extends PageQuery {
    private String keyword;
    private String status;
}
