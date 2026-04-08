package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_scheduled_task")
public class SysScheduledTask implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private String taskType;
    private String executionType;
    private Integer intervalValue;
    private String intervalUnit;
    private String cronExpression;
    private String status;
    private String taskSummary;
    private String taskConfigJson;
    private Integer successNotify;
    private Integer failureNotify;
    private String notifyChannelsJson;
    private String notifyUserIdsJson;
    private Long runCount;
    private String operator;
    private String remark;
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;
    private LocalDateTime lastSuccessTime;
    private LocalDateTime lastFailureTime;
    private String lastErrorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
