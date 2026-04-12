package com.sak.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BlogPostSaveRequest {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200个字符")
    private String title;

    @Size(max = 220, message = "文章别名长度不能超过220个字符")
    private String slug;

    @Size(max = 1000, message = "文章摘要长度不能超过1000个字符")
    private String summary;

    @Size(max = 255, message = "封面图片长度不能超过255个字符")
    private String coverImage;

    private Long categoryId;
    private List<Long> tagIds;

    @NotBlank(message = "文章正文不能为空")
    private String contentMarkdown;

    @Size(max = 255, message = "SEO标题长度不能超过255个字符")
    private String seoTitle;

    @Size(max = 255, message = "SEO关键词长度不能超过255个字符")
    private String seoKeywords;

    @Size(max = 500, message = "SEO描述长度不能超过500个字符")
    private String seoDescription;

    @Size(max = 20, message = "文章来源类型长度不能超过20个字符")
    private String sourceType;

    @Size(max = 500, message = "原文地址长度不能超过500个字符")
    private String sourceUrl;

    private Integer allowComment;
    private Integer isTop;
    private Integer isRecommend;
}
