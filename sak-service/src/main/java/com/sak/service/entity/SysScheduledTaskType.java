package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_scheduled_task_type")
public class SysScheduledTaskType implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private String taskName;
    private String description;
    private String formSchemaJson;
    private Integer builtIn;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
