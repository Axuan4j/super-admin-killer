package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlogPostDetailResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String coverImage;
    private Long categoryId;
    private String categoryName;
    private List<Long> tagIds;
    private String contentMarkdown;
    private String contentHtml;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private String sourceType;
    private String sourceUrl;
    private Integer allowComment;
    private Integer isTop;
    private Integer isRecommend;
    private String status;
    private Integer wordCount;
    private Integer readingMinutes;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
