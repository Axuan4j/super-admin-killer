package com.sak.service.service;

import com.sak.service.dto.PageResponse;
import com.sak.service.dto.ScheduledTaskQueryRequest;
import com.sak.service.dto.ScheduledTaskResponse;
import com.sak.service.dto.ScheduledTaskSaveRequest;
import com.sak.service.dto.ScheduledTaskTypeOptionResponse;

import java.util.List;

public interface ScheduledTaskService {

    PageResponse<ScheduledTaskResponse> listTasks(ScheduledTaskQueryRequest request);

    ScheduledTaskResponse getTask(Long id);

    ScheduledTaskResponse createTask(ScheduledTaskSaveRequest request, String operator);

    ScheduledTaskResponse updateTask(Long id, ScheduledTaskSaveRequest request);

    void startTask(Long id);

    void pauseTask(Long id);

    void cancelTask(Long id);

    void runTask(Long id);

    void deleteTask(Long id);

    List<ScheduledTaskTypeOptionResponse> listTaskTypes();

    List<ScheduledTaskTypeOptionResponse> refreshTaskTypes();

    void restoreScheduledTasks();
}
