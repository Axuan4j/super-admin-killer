package com.sak.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduledTaskResponse {
    private Long id;
    private String taskName;
    private String taskType;
    private String executionType;
    private Integer intervalValue;
    private String intervalUnit;
    private String cronExpression;
    private String status;
    private String taskSummary;
    private JsonNode taskConfig;
    private Boolean successNotify;
    private Boolean failureNotify;
    private List<String> notifyChannels;
    private List<Long> notifyUserIds;
    private Long runCount;
    private String operator;
    private String remark;
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;
    private LocalDateTime lastSuccessTime;
    private LocalDateTime lastFailureTime;
    private String lastErrorMessage;
    private LocalDateTime createTime;
}
