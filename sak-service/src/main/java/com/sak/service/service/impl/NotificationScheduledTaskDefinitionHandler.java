package com.sak.service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.ScheduledTaskFieldOptionResponse;
import com.sak.service.dto.ScheduledTaskFormFieldResponse;
import com.sak.service.dto.ScheduledTaskTypeOptionResponse;
import com.sak.service.service.NotificationDispatchService;
import com.sak.service.service.ScheduledTaskDefinitionHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationScheduledTaskDefinitionHandler implements ScheduledTaskDefinitionHandler {

    private static final String TASK_TYPE = "NOTIFICATION_PUSH";

    private final ObjectMapper objectMapper;
    private final NotificationDispatchService notificationDispatchService;

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public ScheduledTaskTypeOptionResponse getTaskTypeDefinition() {
        return new ScheduledTaskTypeOptionResponse(
                TASK_TYPE,
                "消息推送任务",
                "可按固定频率、固定延迟或 Cron 执行消息推送，复用站内信、邮件、WxPusher 渠道",
                buildFormSchema()
        );
    }

    @Override
    public void validateTaskConfig(JsonNode taskConfig) {
        NotificationTaskConfig config = parseConfig(taskConfig);
        if (!StringUtils.hasText(config.getTitle())) {
            throw new IllegalArgumentException("任务通知标题不能为空");
        }
        if (!StringUtils.hasText(config.getContent())) {
            throw new IllegalArgumentException("任务通知内容不能为空");
        }
        if (config.getChannels() == null || config.getChannels().isEmpty()) {
            throw new IllegalArgumentException("任务至少需要选择一种推送渠道");
        }
        if (!Boolean.TRUE.equals(config.getSendAll()) && (config.getUserIds() == null || config.getUserIds().isEmpty())) {
            throw new IllegalArgumentException("非全员推送时，请至少选择一个接收用户");
        }
    }

    @Override
    public String buildTaskSummary(JsonNode taskConfig) {
        NotificationTaskConfig config = parseConfig(taskConfig);
        String target = Boolean.TRUE.equals(config.getSendAll())
                ? "全部启用用户"
                : "指定用户 " + (config.getUserIds() == null ? 0 : config.getUserIds().size()) + " 人";
        return config.getTitle() + " / " + target;
    }

    @Override
    public void execute(JsonNode taskConfig) {
        NotificationTaskConfig config = parseConfig(taskConfig);
        NotificationSendRequest request = new NotificationSendRequest();
        request.setSendAll(Boolean.TRUE.equals(config.getSendAll()));
        request.setUserIds(config.getUserIds());
        request.setChannels(config.getChannels());
        request.setTitle(config.getTitle());
        request.setContent(config.getContent());
        notificationDispatchService.dispatch("调度中心", request);
    }

    private NotificationTaskConfig parseConfig(JsonNode taskConfig) {
        if (taskConfig == null || taskConfig.isNull()) {
            throw new IllegalArgumentException("任务配置不能为空");
        }
        return objectMapper.convertValue(taskConfig, NotificationTaskConfig.class);
    }

    private List<ScheduledTaskFormFieldResponse> buildFormSchema() {
        List<ScheduledTaskFormFieldResponse> fields = new ArrayList<>();

        ScheduledTaskFormFieldResponse sendAllField = new ScheduledTaskFormFieldResponse();
        sendAllField.setField("sendAll");
        sendAllField.setLabel("发送范围");
        sendAllField.setComponent("radio-group");
        sendAllField.setRequired(true);
        sendAllField.setDefaultValue(Boolean.TRUE);
        sendAllField.setOptions(List.of(
                new ScheduledTaskFieldOptionResponse("发送给全部启用用户", true),
                new ScheduledTaskFieldOptionResponse("发送给指定用户", false)
        ));
        fields.add(sendAllField);

        ScheduledTaskFormFieldResponse userIdsField = new ScheduledTaskFormFieldResponse();
        userIdsField.setField("userIds");
        userIdsField.setLabel("接收用户");
        userIdsField.setComponent("user-select");
        userIdsField.setPlaceholder("请选择一个或多个接收用户");
        userIdsField.setDefaultValue(List.of());
        userIdsField.setVisibleWhenField("sendAll");
        userIdsField.setVisibleWhenEquals(false);
        fields.add(userIdsField);

        ScheduledTaskFormFieldResponse channelsField = new ScheduledTaskFormFieldResponse();
        channelsField.setField("channels");
        channelsField.setLabel("推送渠道");
        channelsField.setComponent("checkbox-group");
        channelsField.setRequired(true);
        channelsField.setDefaultValue(List.of("SITE_MESSAGE"));
        channelsField.setOptions(List.of(
                new ScheduledTaskFieldOptionResponse("站内信", "SITE_MESSAGE"),
                new ScheduledTaskFieldOptionResponse("邮件", "EMAIL"),
                new ScheduledTaskFieldOptionResponse("WxPusher", "WXPUSHER")
        ));
        channelsField.setHelpText("至少选择一种推送渠道");
        fields.add(channelsField);

        ScheduledTaskFormFieldResponse titleField = new ScheduledTaskFormFieldResponse();
        titleField.setField("title");
        titleField.setLabel("通知标题");
        titleField.setComponent("input");
        titleField.setRequired(true);
        titleField.setMaxLength(120);
        titleField.setPlaceholder("请输入通知标题");
        titleField.setDefaultValue("");
        fields.add(titleField);

        ScheduledTaskFormFieldResponse contentField = new ScheduledTaskFormFieldResponse();
        contentField.setField("content");
        contentField.setLabel("通知内容");
        contentField.setComponent("textarea");
        contentField.setRequired(true);
        contentField.setMaxLength(1000);
        contentField.setMinRows(5);
        contentField.setMaxRows(9);
        contentField.setPlaceholder("请输入通知内容");
        contentField.setDefaultValue("");
        fields.add(contentField);

        return fields;
    }

    @Data
    private static class NotificationTaskConfig {
        private Boolean sendAll;
        private List<Long> userIds;
        private List<String> channels;
        private String title;
        private String content;
    }
}
