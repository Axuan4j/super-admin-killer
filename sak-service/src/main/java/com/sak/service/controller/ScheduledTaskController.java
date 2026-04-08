package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.ScheduledTaskQueryRequest;
import com.sak.service.dto.ScheduledTaskResponse;
import com.sak.service.dto.ScheduledTaskSaveRequest;
import com.sak.service.dto.ScheduledTaskTypeOptionResponse;
import com.sak.service.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/schedules")
@RequiredArgsConstructor
public class ScheduledTaskController {

    private final ScheduledTaskService scheduledTaskService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:schedule:view')")
    public Result<PageResponse<ScheduledTaskResponse>> listTasks(ScheduledTaskQueryRequest request) {
        return Result.success(scheduledTaskService.listTasks(request));
    }

    @GetMapping("/task-types")
    @PreAuthorize("hasAuthority('system:schedule:view')")
    public Result<List<ScheduledTaskTypeOptionResponse>> listTaskTypes() {
        return Result.success(scheduledTaskService.listTaskTypes());
    }

    @PostMapping("/task-types/refresh")
    @PreAuthorize("hasAuthority('system:schedule:edit')")
    public Result<List<ScheduledTaskTypeOptionResponse>> refreshTaskTypes() {
        return Result.success(scheduledTaskService.refreshTaskTypes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:schedule:view')")
    public Result<ScheduledTaskResponse> getTask(@PathVariable("id") Long id) {
        return Result.success(scheduledTaskService.getTask(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:schedule:add')")
    public Result<ScheduledTaskResponse> createTask(Authentication authentication, @RequestBody ScheduledTaskSaveRequest request) {
        String operator = authentication == null ? "系统" : authentication.getName();
        return Result.success(scheduledTaskService.createTask(request, operator));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:schedule:edit')")
    public Result<ScheduledTaskResponse> updateTask(@PathVariable("id") Long id, @RequestBody ScheduledTaskSaveRequest request) {
        return Result.success(scheduledTaskService.updateTask(id, request));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('system:schedule:edit')")
    public Result<Void> startTask(@PathVariable("id") Long id) {
        scheduledTaskService.startTask(id);
        return Result.success();
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('system:schedule:edit')")
    public Result<Void> pauseTask(@PathVariable("id") Long id) {
        scheduledTaskService.pauseTask(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('system:schedule:edit')")
    public Result<Void> cancelTask(@PathVariable("id") Long id) {
        scheduledTaskService.cancelTask(id);
        return Result.success();
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("hasAuthority('system:schedule:run')")
    public Result<Void> runTask(@PathVariable("id") Long id) {
        scheduledTaskService.runTask(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:schedule:remove')")
    public Result<Void> deleteTask(@PathVariable("id") Long id) {
        scheduledTaskService.deleteTask(id);
        return Result.success();
    }
}
