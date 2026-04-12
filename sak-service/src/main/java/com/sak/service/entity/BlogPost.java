package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("blog_post")
public class BlogPost implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String coverImage;
    private Long categoryId;
    private Long authorUserId;
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
    private LocalDateTime publishTime;
    private LocalDateTime lastCommentTime;
    private Integer wordCount;
    private Integer readingMinutes;
    private Integer versionNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
