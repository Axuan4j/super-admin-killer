package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_site_message")
public class SysSiteMessage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String senderName;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
