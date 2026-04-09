package com.sak.service.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.sak.service.dto.ScheduledTaskTypeOptionResponse;

public interface ScheduledTaskDefinitionHandler {

    String getTaskType();

    ScheduledTaskTypeOptionResponse getTaskTypeDefinition();

    void validateTaskConfig(JsonNode taskConfig);

    String buildTaskSummary(JsonNode taskConfig);

    void execute(JsonNode taskConfig);
}
