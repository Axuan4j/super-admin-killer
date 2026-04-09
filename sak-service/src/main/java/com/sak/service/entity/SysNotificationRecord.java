package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_notification_record")
public class SysNotificationRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String senderName;
    private String title;
    private String content;
    private String channelsJson;
    private Integer sendAll;
    private Integer recipientCount;
    private Integer successUserCount;
    private String status;
    private String channelSuccessCountsJson;
    private String channelSkipCountsJson;
    private String recipientDetailsJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
