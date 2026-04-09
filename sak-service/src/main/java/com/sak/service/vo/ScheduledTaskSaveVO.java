package com.sak.service.vo;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ScheduledTaskSaveVO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称长度不能超过128位")
    private String taskName;

    @NotBlank(message = "任务类型不能为空")
    @Size(max = 64, message = "任务类型长度不能超过64位")
    private String taskType;

    @NotBlank(message = "执行方式不能为空")
    @Pattern(regexp = "ONCE|INTERVAL|CRON", message = "执行方式不合法")
    private String executionType;

    private Integer intervalValue;

    @Size(max = 32, message = "间隔单位长度不能超过32位")
    private String intervalUnit;

    @Size(max = 128, message = "Cron表达式长度不能超过128位")
    private String cronExpression;

    @Pattern(regexp = "SCHEDULED|PAUSED|CANCELED", message = "任务状态不合法")
    private String status;

    private JsonNode taskConfig;

    private Boolean successNotify;

    private Boolean failureNotify;

    private List<String> notifyChannels;

    private List<Long> notifyUserIds;

    @Size(max = 255, message = "备注长度不能超过255位")
    private String remark;
}
