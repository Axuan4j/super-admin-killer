package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("blog_category")
public class BlogCategory implements Serializable {
    @TableId(type = IdType.AUTO)
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
