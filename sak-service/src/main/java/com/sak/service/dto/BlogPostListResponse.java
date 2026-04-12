package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogPostListResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String coverImage;
    private String status;
    private Long categoryId;
    private String categoryName;
    private Integer allowComment;
    private Integer isTop;
    private Integer isRecommend;
    private Integer wordCount;
    private Integer readingMinutes;
    private LocalDateTime publishTime;
    private LocalDateTime updateTime;
}
