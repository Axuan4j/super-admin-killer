package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("blog_post_stat")
public class BlogPostStat implements Serializable {
    @TableId
    private Long postId;
    private Long viewCount;
    private Long uniqueViewCount;
    private Long commentCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long shareCount;
    private LocalDateTime lastViewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
