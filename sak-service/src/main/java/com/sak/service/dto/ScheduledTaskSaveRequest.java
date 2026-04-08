package com.sak.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class ScheduledTaskSaveRequest {
    private String taskName;
    private String taskType;
    private String executionType;
    private Integer intervalValue;
    private String intervalUnit;
    private String cronExpression;
    private String status;
    private JsonNode taskConfig;
    private Boolean successNotify;
    private Boolean failureNotify;
    private List<String> notifyChannels;
    private List<Long> notifyUserIds;
    private String remark;
}
