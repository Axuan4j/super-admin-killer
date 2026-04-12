package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("blog_tag")
public class BlogTag implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String slug;
    private String color;
    private String description;
    private Integer orderNum;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
