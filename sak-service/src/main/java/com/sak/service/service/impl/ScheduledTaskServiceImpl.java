package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.ScheduledTaskFormFieldResponse;
import com.sak.service.dto.ScheduledTaskQueryRequest;
import com.sak.service.dto.ScheduledTaskResponse;
import com.sak.service.dto.ScheduledTaskSaveRequest;
import com.sak.service.dto.ScheduledTaskTypeOptionResponse;
import com.sak.service.entity.SysScheduledTask;
import com.sak.service.entity.SysScheduledTaskType;
import com.sak.service.mapper.SysScheduledTaskMapper;
import com.sak.service.mapper.SysScheduledTaskTypeMapper;
import com.sak.service.service.NotificationDispatchService;
import com.sak.service.job.ScheduledTaskDefinitionHandler;
import com.sak.service.service.ScheduledTaskService;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_PAUSED = "PAUSED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_CANCELED = "CANCELED";

    private static final String EXECUTION_CRON = "CRON";
    private static final String EXECUTION_FIXED_RATE = "FIXED_RATE";
    private static final String EXECUTION_FIXED_DELAY = "FIXED_DELAY";

    private static final Map<String, String> TASK_SORT_FIELDS = new LinkedHashMap<>();
    private static final Map<String, ChronoUnit> INTERVAL_UNITS = new LinkedHashMap<>();

    static {
        TASK_SORT_FIELDS.put("id", "id");
        TASK_SORT_FIELDS.put("taskName", "task_name");
        TASK_SORT_FIELDS.put("taskType", "task_type");
        TASK_SORT_FIELDS.put("executionType", "execution_type");
        TASK_SORT_FIELDS.put("status", "status");
        TASK_SORT_FIELDS.put("runCount", "run_count");
        TASK_SORT_FIELDS.put("lastRunTime", "last_run_time");
        TASK_SORT_FIELDS.put("nextRunTime", "next_run_time");
        TASK_SORT_FIELDS.put("createTime", "create_time");

        INTERVAL_UNITS.put("SECONDS", ChronoUnit.SECONDS);
        INTERVAL_UNITS.put("MINUTES", ChronoUnit.MINUTES);
        INTERVAL_UNITS.put("HOURS", ChronoUnit.HOURS);
        INTERVAL_UNITS.put("DAYS", ChronoUnit.DAYS);
    }

    private final SysScheduledTaskMapper sysScheduledTaskMapper;
    private final SysScheduledTaskTypeMapper sysScheduledTaskTypeMapper;
    private final List<ScheduledTaskDefinitionHandler> taskDefinitionHandlers;
    private final ThreadPoolTaskScheduler scheduleTaskScheduler;
    private final NotificationDispatchService notificationDispatchService;
    private final ObjectMapper objectMapper;

    private final Map<Long, RuntimeTask> runtimeTasks = new ConcurrentHashMap<>();

    @Override
    public PageResponse<ScheduledTaskResponse> listTasks(ScheduledTaskQueryRequest request) {
        LambdaQueryWrapper<SysScheduledTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(SysScheduledTask::getStatus, request.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getTaskType())) {
            wrapper.eq(SysScheduledTask::getTaskType, request.getTaskType().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            wrapper.and(q -> q.like(SysScheduledTask::getTaskName, keyword)
                    .or()
                    .like(SysScheduledTask::getTaskSummary, keyword)
                    .or()
                    .like(SysScheduledTask::getRemark, keyword));
        }
        Page<SysScheduledTask> page = sysScheduledTaskMapper.selectPage(
                PageUtils.buildPage(request, TASK_SORT_FIELDS, "id", "desc"),
                wrapper
        );
        return PageUtils.toResponse(page, page.getRecords().stream().map(this::toResponse).toList(), request);
    }

    @Override
    public ScheduledTaskResponse getTask(Long id) {
        return toResponse(requireTask(id));
    }

    @Override
    @Transactional
    @LogRecord(success = "新增调度任务：{{#p0.taskName}}", fail = "新增调度任务失败：{{#p0.taskName}}", type = "SCHEDULE", subType = "CREATE", bizNo = "{{#p0.taskName}}")
    public ScheduledTaskResponse createTask(ScheduledTaskSaveRequest request, String operator) {
        ScheduledTaskDefinitionHandler handler = resolveHandler(request.getTaskType());
        validateRequest(request, handler);

        String normalizedStatus = normalizeEditableStatus(request.getStatus());
        String executionType = normalizeExecutionType(request.getExecutionType());
        Integer intervalValue = normalizeIntervalValue(request.getIntervalValue(), executionType);
        String intervalUnit = normalizeIntervalUnit(request.getIntervalUnit(), executionType);

        SysScheduledTask task = new SysScheduledTask();
        LocalDateTime now = LocalDateTime.now();
        task.setTaskName(request.getTaskName().trim());
        task.setTaskType(handler.getTaskType());
        task.setExecutionType(executionType);
        task.setIntervalValue(intervalValue);
        task.setIntervalUnit(intervalUnit);
        task.setCronExpression(EXECUTION_CRON.equals(executionType) ? request.getCronExpression().trim() : null);
        task.setStatus(normalizedStatus);
        task.setTaskSummary(handler.buildTaskSummary(request.getTaskConfig()));
        task.setTaskConfigJson(writeJson(request.getTaskConfig()));
        task.setSuccessNotify(Boolean.TRUE.equals(request.getSuccessNotify()) ? 1 : 0);
        task.setFailureNotify(Boolean.TRUE.equals(request.getFailureNotify()) ? 1 : 0);
        task.setNotifyChannelsJson(writeJson(normalizeNotifyChannels(request.getNotifyChannels())));
        task.setNotifyUserIdsJson(writeJson(normalizeNotifyUserIds(request.getNotifyUserIds())));
        task.setRunCount(0L);
        task.setOperator(StringUtils.hasText(operator) ? operator.trim() : "系统");
        task.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setNextRunTime(STATUS_SCHEDULED.equals(normalizedStatus) ? calculateNextRunTime(task, now) : null);
        sysScheduledTaskMapper.insert(task);

        if (STATUS_SCHEDULED.equals(task.getStatus())) {
            scheduleTask(task);
        }
        return toResponse(task);
    }

    @Override
    @Transactional
    @LogRecord(success = "编辑调度任务：{{#p1.taskName}}", fail = "编辑调度任务失败：{{#p1.taskName}}", type = "SCHEDULE", subType = "UPDATE", bizNo = "{{#p0}}")
    public ScheduledTaskResponse updateTask(Long id, ScheduledTaskSaveRequest request) {
        SysScheduledTask existingTask = requireTask(id);
        ensureNotRunning(existingTask.getId(), "任务执行中，暂不允许编辑");

        ScheduledTaskDefinitionHandler handler = resolveHandler(request.getTaskType());
        validateRequest(request, handler);

        String normalizedStatus = normalizeEditableStatus(request.getStatus());
        String executionType = normalizeExecutionType(request.getExecutionType());
        Integer intervalValue = normalizeIntervalValue(request.getIntervalValue(), executionType);
        String intervalUnit = normalizeIntervalUnit(request.getIntervalUnit(), executionType);

        existingTask.setTaskName(request.getTaskName().trim());
        existingTask.setTaskType(handler.getTaskType());
        existingTask.setExecutionType(executionType);
        existingTask.setIntervalValue(intervalValue);
        existingTask.setIntervalUnit(intervalUnit);
        existingTask.setCronExpression(EXECUTION_CRON.equals(executionType) ? request.getCronExpression().trim() : null);
        existingTask.setStatus(normalizedStatus);
        existingTask.setTaskSummary(handler.buildTaskSummary(request.getTaskConfig()));
        existingTask.setTaskConfigJson(writeJson(request.getTaskConfig()));
        existingTask.setSuccessNotify(Boolean.TRUE.equals(request.getSuccessNotify()) ? 1 : 0);
        existingTask.setFailureNotify(Boolean.TRUE.equals(request.getFailureNotify()) ? 1 : 0);
        existingTask.setNotifyChannelsJson(writeJson(normalizeNotifyChannels(request.getNotifyChannels())));
        existingTask.setNotifyUserIdsJson(writeJson(normalizeNotifyUserIds(request.getNotifyUserIds())));
        existingTask.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        existingTask.setNextRunTime(STATUS_SCHEDULED.equals(normalizedStatus) ? calculateNextRunTime(existingTask, LocalDateTime.now()) : null);
        existingTask.setLastErrorMessage(null);
        sysScheduledTaskMapper.updateById(existingTask);

        unscheduleTask(existingTask.getId());
        if (STATUS_SCHEDULED.equals(existingTask.getStatus())) {
            scheduleTask(existingTask);
        }
        return toResponse(existingTask);
    }

    @Override
    @Transactional
    public void startTask(Long id) {
        SysScheduledTask task = requireTask(id);
        ensureNotRunning(id, "任务执行中，无需再次启动");
        task.setStatus(STATUS_SCHEDULED);
        task.setNextRunTime(calculateNextRunTime(task, LocalDateTime.now()));
        task.setLastErrorMessage(null);
        sysScheduledTaskMapper.updateById(task);
        scheduleTask(task);
    }

    @Override
    @Transactional
    public void pauseTask(Long id) {
        SysScheduledTask task = requireTask(id);
        ensureNotRunning(id, "任务执行中，无法暂停");
        unscheduleTask(id);
        task.setStatus(STATUS_PAUSED);
        task.setNextRunTime(null);
        sysScheduledTaskMapper.updateById(task);
    }

    @Override
    @Transactional
    public void cancelTask(Long id) {
        SysScheduledTask task = requireTask(id);
        ensureNotRunning(id, "任务执行中，无法取消");
        unscheduleTask(id);
        task.setStatus(STATUS_CANCELED);
        task.setNextRunTime(null);
        sysScheduledTaskMapper.updateById(task);
    }

    @Override
    public void runTask(Long id) {
        SysScheduledTask task = requireTask(id);
        if (STATUS_CANCELED.equals(task.getStatus())) {
            throw new IllegalArgumentException("已取消的任务不可执行");
        }
        ensureNotRunning(id, "任务执行中，请稍后再试");
        scheduleTaskScheduler.execute(() -> executeTask(id, true));
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        ensureNotRunning(id, "任务执行中，无法删除");
        unscheduleTask(id);
        sysScheduledTaskMapper.deleteById(id);
    }

    @Override
    public List<ScheduledTaskTypeOptionResponse> listTaskTypes() {
        List<ScheduledTaskTypeOptionResponse> options = listTaskTypesFromDatabase();
        if (!options.isEmpty()) {
            return options;
        }
        return refreshTaskTypes();
    }

    @Override
    @Transactional
    public List<ScheduledTaskTypeOptionResponse> refreshTaskTypes() {
        Map<String, ScheduledTaskDefinitionHandler> handlerMap = buildHandlerMap();
        LocalDateTime now = LocalDateTime.now();

        List<SysScheduledTaskType> existingRows = sysScheduledTaskTypeMapper.selectList(new LambdaQueryWrapper<SysScheduledTaskType>()
                .orderByAsc(SysScheduledTaskType::getTaskType));
        Map<String, SysScheduledTaskType> existingMap = existingRows.stream()
                .collect(Collectors.toMap(SysScheduledTaskType::getTaskType, row -> row, (left, right) -> left, LinkedHashMap::new));

        for (ScheduledTaskDefinitionHandler handler : handlerMap.values()) {
            ScheduledTaskTypeOptionResponse definition = handler.getTaskTypeDefinition();
            SysScheduledTaskType row = existingMap.get(handler.getTaskType());
            if (row == null) {
                row = new SysScheduledTaskType();
                row.setTaskType(handler.getTaskType());
                row.setCreateTime(now);
            }
            row.setTaskName(definition.getName());
            row.setDescription(definition.getDescription());
            row.setFormSchemaJson(writeJson(definition.getFormSchema() == null ? Collections.emptyList() : definition.getFormSchema()));
            row.setBuiltIn(1);
            row.setEnabled(1);
            row.setUpdateTime(now);
            if (row.getId() == null) {
                sysScheduledTaskTypeMapper.insert(row);
            } else {
                sysScheduledTaskTypeMapper.updateById(row);
            }
        }

        existingRows.stream()
                .filter(row -> row.getBuiltIn() != null && row.getBuiltIn() == 1)
                .filter(row -> !handlerMap.containsKey(row.getTaskType()))
                .forEach(row -> {
                    row.setEnabled(0);
                    row.setUpdateTime(now);
                    sysScheduledTaskTypeMapper.updateById(row);
                });

        return listTaskTypesFromDatabase();
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void restoreScheduledTasks() {
        refreshTaskTypes();

        List<SysScheduledTask> tasks = sysScheduledTaskMapper.selectList(new LambdaQueryWrapper<SysScheduledTask>()
                .eq(SysScheduledTask::getStatus, STATUS_SCHEDULED)
                .orderByAsc(SysScheduledTask::getId));
        tasks.forEach(task -> {
            try {
                scheduleTask(task);
            } catch (Exception ex) {
                log.warn("restore scheduled task failed, taskId={}", task.getId(), ex);
                markTaskPausedOnRestoreFailure(task.getId(), ex.getMessage());
            }
        });
    }

    private List<ScheduledTaskTypeOptionResponse> listTaskTypesFromDatabase() {
        return sysScheduledTaskTypeMapper.selectList(new LambdaQueryWrapper<SysScheduledTaskType>()
                        .eq(SysScheduledTaskType::getEnabled, 1)
                        .orderByAsc(SysScheduledTaskType::getTaskName))
                .stream()
                .map(this::toTaskTypeResponse)
                .toList();
    }

    private ScheduledTaskTypeOptionResponse toTaskTypeResponse(SysScheduledTaskType row) {
        return new ScheduledTaskTypeOptionResponse(
                row.getTaskType(),
                row.getTaskName(),
                row.getDescription(),
                readFormSchema(row.getFormSchemaJson())
        );
    }

    private List<ScheduledTaskFormFieldResponse> readFormSchema(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ScheduledTaskFormFieldResponse.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析任务类型表单失败", ex);
        }
    }

    private Map<String, ScheduledTaskDefinitionHandler> buildHandlerMap() {
        Map<String, ScheduledTaskDefinitionHandler> handlerMap = new LinkedHashMap<>();
        taskDefinitionHandlers.stream()
                .sorted(Comparator.comparing(ScheduledTaskDefinitionHandler::getTaskType, String.CASE_INSENSITIVE_ORDER))
                .forEach(handler -> {
                    String taskType = handler.getTaskType().trim().toUpperCase();
                    if (handlerMap.putIfAbsent(taskType, handler) != null) {
                        throw new IllegalStateException("存在重复的调度任务类型：" + taskType);
                    }
                });
        return handlerMap;
    }

    private void validateRequest(ScheduledTaskSaveRequest request, ScheduledTaskDefinitionHandler handler) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getTaskName())) {
            throw new IllegalArgumentException("任务名称不能为空");
        }

        String executionType = normalizeExecutionType(request.getExecutionType());
        if (EXECUTION_CRON.equals(executionType)) {
            if (!StringUtils.hasText(request.getCronExpression())) {
                throw new IllegalArgumentException("Cron 表达式不能为空");
            }
            parseCronExpression(request.getCronExpression().trim());
        } else {
            normalizeIntervalValue(request.getIntervalValue(), executionType);
            normalizeIntervalUnit(request.getIntervalUnit(), executionType);
        }

        handler.validateTaskConfig(request.getTaskConfig());

        boolean successNotify = Boolean.TRUE.equals(request.getSuccessNotify());
        boolean failureNotify = Boolean.TRUE.equals(request.getFailureNotify());
        if (successNotify || failureNotify) {
            if (normalizeNotifyChannels(request.getNotifyChannels()).isEmpty()) {
                throw new IllegalArgumentException("启用执行通知时，请至少选择一种通知渠道");
            }
            if (normalizeNotifyUserIds(request.getNotifyUserIds()).isEmpty()) {
                throw new IllegalArgumentException("启用执行通知时，请至少选择一个接收用户");
            }
        }
    }

    private void scheduleTask(SysScheduledTask task) {
        ScheduledTaskDefinitionHandler handler = resolveHandler(task.getTaskType());
        handler.validateTaskConfig(readJsonNode(task.getTaskConfigJson()));
        unscheduleTask(task.getId());

        String executionType = normalizeExecutionType(task.getExecutionType());
        ScheduledFuture<?> future;
        if (EXECUTION_CRON.equals(executionType)) {
            future = scheduleTaskScheduler.schedule(
                    () -> executeTask(task.getId(), false),
                    new CronTrigger(task.getCronExpression())
            );
        } else {
            Duration interval = resolveInterval(task.getIntervalValue(), task.getIntervalUnit());
            Instant startTime = Instant.now().plus(interval);
            if (EXECUTION_FIXED_RATE.equals(executionType)) {
                future = scheduleTaskScheduler.scheduleAtFixedRate(
                        () -> executeTask(task.getId(), false),
                        startTime,
                        interval
                );
            } else {
                future = scheduleTaskScheduler.scheduleWithFixedDelay(
                        () -> executeTask(task.getId(), false),
                        startTime,
                        interval
                );
            }
        }

        RuntimeTask runtimeTask = new RuntimeTask(task.getId(), future);
        runtimeTasks.put(task.getId(), runtimeTask);

        task.setStatus(STATUS_SCHEDULED);
        task.setNextRunTime(calculateNextRunTime(task, LocalDateTime.now()));
        sysScheduledTaskMapper.updateById(task);
    }

    private void executeTask(Long taskId, boolean manual) {
        RuntimeTask runtimeTask = runtimeTasks.computeIfAbsent(taskId, RuntimeTask::new);
        if (!runtimeTask.running.compareAndSet(false, true)) {
            return;
        }

        SysScheduledTask task = requireTask(taskId);
        String previousStatus = task.getStatus();
        LocalDateTime runTime = LocalDateTime.now();
        task.setStatus(STATUS_RUNNING);
        task.setLastRunTime(runTime);
        task.setNextRunTime(null);
        sysScheduledTaskMapper.updateById(task);

        boolean success = false;
        String errorMessage = null;
        try {
            resolveHandler(task.getTaskType()).execute(readJsonNode(task.getTaskConfigJson()));
            success = true;
        } catch (Exception ex) {
            errorMessage = buildRootMessage(ex);
            log.warn("execute scheduled task failed, taskId={}", taskId, ex);
        } finally {
            LocalDateTime finishedAt = LocalDateTime.now();
            SysScheduledTask latestTask = requireTask(taskId);
            latestTask.setRunCount((latestTask.getRunCount() == null ? 0L : latestTask.getRunCount()) + 1);
            latestTask.setLastRunTime(runTime);
            latestTask.setStatus(resolveFinalStatus(previousStatus, manual));
            latestTask.setNextRunTime(resolveNextRunTimeAfterExecution(latestTask, runTime, finishedAt, manual));

            if (success) {
                latestTask.setLastSuccessTime(finishedAt);
                latestTask.setLastErrorMessage(null);
            } else {
                latestTask.setLastFailureTime(finishedAt);
                latestTask.setLastErrorMessage(errorMessage);
            }
            sysScheduledTaskMapper.updateById(latestTask);

            sendExecutionNotification(latestTask, success, errorMessage, runTime, manual);

            if (!STATUS_SCHEDULED.equals(latestTask.getStatus())) {
                unscheduleTask(taskId);
            }
            runtimeTask.running.set(false);
        }
    }

    private LocalDateTime resolveNextRunTimeAfterExecution(SysScheduledTask task, LocalDateTime runTime, LocalDateTime finishedAt, boolean manual) {
        if (!STATUS_SCHEDULED.equals(task.getStatus())) {
            return null;
        }
        if (manual) {
            return calculateNextRunTime(task, LocalDateTime.now());
        }
        String executionType = normalizeExecutionType(task.getExecutionType());
        if (EXECUTION_FIXED_DELAY.equals(executionType)) {
            return calculateNextRunTime(task, finishedAt);
        }
        if (EXECUTION_FIXED_RATE.equals(executionType)) {
            return calculateNextRunTime(task, runTime);
        }
        return calculateNextRunTime(task, finishedAt);
    }

    private String resolveFinalStatus(String previousStatus, boolean manual) {
        if (!manual) {
            return STATUS_SCHEDULED;
        }
        if (STATUS_PAUSED.equals(previousStatus)) {
            return STATUS_PAUSED;
        }
        return STATUS_SCHEDULED;
    }

    private void sendExecutionNotification(SysScheduledTask task, boolean success, String errorMessage, LocalDateTime runTime, boolean manual) {
        boolean shouldNotify = success ? task.getSuccessNotify() != null && task.getSuccessNotify() == 1
                : task.getFailureNotify() != null && task.getFailureNotify() == 1;
        if (!shouldNotify) {
            return;
        }

        List<String> channels = readStringList(task.getNotifyChannelsJson());
        List<Long> userIds = readLongList(task.getNotifyUserIdsJson());
        if (channels.isEmpty() || userIds.isEmpty()) {
            return;
        }

        NotificationSendRequest request = new NotificationSendRequest();
        request.setSendAll(false);
        request.setUserIds(userIds);
        request.setChannels(channels);
        request.setTitle("[调度中心] " + task.getTaskName() + (success ? " 执行成功" : " 执行失败"));
        request.setContent(buildExecutionNotificationContent(task, success, errorMessage, runTime, manual));
        try {
            notificationDispatchService.dispatch("调度中心", request);
        } catch (Exception ex) {
            log.warn("send scheduled task notification failed, taskId={}", task.getId(), ex);
        }
    }

    private String buildExecutionNotificationContent(SysScheduledTask task, boolean success, String errorMessage, LocalDateTime runTime, boolean manual) {
        List<String> lines = new ArrayList<>();
        lines.add("任务名称：" + task.getTaskName());
        lines.add("任务类型：" + task.getTaskType());
        lines.add("执行方式：" + (manual ? "手动触发" : buildExecutionSummary(task)));
        lines.add("任务摘要：" + task.getTaskSummary());
        lines.add("执行时间：" + runTime);
        lines.add("执行结果：" + (success ? "成功" : "失败"));
        if (!success && StringUtils.hasText(errorMessage)) {
            lines.add("错误信息：" + errorMessage);
        }
        return String.join("\n", lines);
    }

    private void unscheduleTask(Long taskId) {
        RuntimeTask runtimeTask = runtimeTasks.remove(taskId);
        if (runtimeTask != null && runtimeTask.future != null) {
            runtimeTask.future.cancel(false);
        }
    }

    private void ensureNotRunning(Long taskId, String message) {
        RuntimeTask runtimeTask = runtimeTasks.get(taskId);
        if (runtimeTask != null && runtimeTask.running.get()) {
            throw new IllegalStateException(message);
        }
    }

    private SysScheduledTask requireTask(Long id) {
        SysScheduledTask task = sysScheduledTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("调度任务不存在");
        }
        return task;
    }

    private ScheduledTaskDefinitionHandler resolveHandler(String taskType) {
        if (!StringUtils.hasText(taskType)) {
            throw new IllegalArgumentException("任务类型不能为空");
        }
        String normalizedType = taskType.trim().toUpperCase();
        return buildHandlerMap().values().stream()
                .filter(handler -> normalizedType.equalsIgnoreCase(handler.getTaskType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的任务类型：" + taskType));
    }

    private String normalizeEditableStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_PAUSED;
        }
        String normalized = status.trim().toUpperCase();
        Set<String> allowed = Set.of(STATUS_SCHEDULED, STATUS_PAUSED);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("仅支持设置为 SCHEDULED 或 PAUSED");
        }
        return normalized;
    }

    private String normalizeExecutionType(String executionType) {
        if (!StringUtils.hasText(executionType)) {
            return EXECUTION_CRON;
        }
        String normalized = executionType.trim().toUpperCase();
        Set<String> allowed = Set.of(EXECUTION_CRON, EXECUTION_FIXED_RATE, EXECUTION_FIXED_DELAY);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("不支持的执行方式：" + executionType);
        }
        return normalized;
    }

    private Integer normalizeIntervalValue(Integer intervalValue, String executionType) {
        if (EXECUTION_CRON.equals(executionType)) {
            return null;
        }
        if (intervalValue == null || intervalValue <= 0) {
            throw new IllegalArgumentException("间隔数值必须大于 0");
        }
        return intervalValue;
    }

    private String normalizeIntervalUnit(String intervalUnit, String executionType) {
        if (EXECUTION_CRON.equals(executionType)) {
            return null;
        }
        if (!StringUtils.hasText(intervalUnit)) {
            throw new IllegalArgumentException("请选择时间单位");
        }
        String normalized = intervalUnit.trim().toUpperCase();
        if (!INTERVAL_UNITS.containsKey(normalized)) {
            throw new IllegalArgumentException("不支持的时间单位：" + intervalUnit);
        }
        return normalized;
    }

    private CronExpression parseCronExpression(String cronExpression) {
        try {
            return CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cron 表达式格式不正确");
        }
    }

    private Duration resolveInterval(Integer intervalValue, String intervalUnit) {
        ChronoUnit unit = INTERVAL_UNITS.get(normalizeIntervalUnit(intervalUnit, EXECUTION_FIXED_RATE));
        return Duration.of(intervalValue, unit);
    }

    private LocalDateTime calculateNextRunTime(SysScheduledTask task, LocalDateTime baseTime) {
        String executionType = normalizeExecutionType(task.getExecutionType());
        if (EXECUTION_CRON.equals(executionType)) {
            return parseCronExpression(task.getCronExpression()).next(baseTime);
        }
        Duration interval = resolveInterval(task.getIntervalValue(), task.getIntervalUnit());
        return baseTime.plus(interval);
    }

    private String buildExecutionSummary(SysScheduledTask task) {
        String executionType = normalizeExecutionType(task.getExecutionType());
        if (EXECUTION_CRON.equals(executionType)) {
            return "Cron：" + task.getCronExpression();
        }
        String unitText = switch (normalizeIntervalUnit(task.getIntervalUnit(), executionType)) {
            case "SECONDS" -> "秒";
            case "MINUTES" -> "分钟";
            case "HOURS" -> "小时";
            case "DAYS" -> "天";
            default -> task.getIntervalUnit();
        };
        if (EXECUTION_FIXED_DELAY.equals(executionType)) {
            return "完成后延迟 " + task.getIntervalValue() + " " + unitText;
        }
        return "每隔 " + task.getIntervalValue() + " " + unitText;
    }

    private JsonNode readJsonNode(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析任务配置失败", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyList() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化任务配置失败", ex);
        }
    }

    private List<String> normalizeNotifyChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return Collections.emptyList();
        }
        return channels.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase())
                .distinct()
                .toList();
    }

    private List<Long> normalizeNotifyUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new LinkedHashSet<>(userIds));
    }

    private List<String> readStringList(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            List<String> values = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return values.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析通知渠道失败", ex);
        }
    }

    private List<Long> readLongList(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            List<Long> values = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
            return values.stream()
                    .filter(value -> value != null && value > 0)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析通知用户失败", ex);
        }
    }

    private ScheduledTaskResponse toResponse(SysScheduledTask task) {
        ScheduledTaskResponse response = new ScheduledTaskResponse();
        response.setId(task.getId());
        response.setTaskName(task.getTaskName());
        response.setTaskType(task.getTaskType());
        response.setExecutionType(normalizeExecutionType(task.getExecutionType()));
        response.setIntervalValue(task.getIntervalValue());
        response.setIntervalUnit(task.getIntervalUnit());
        response.setCronExpression(task.getCronExpression());
        response.setStatus(task.getStatus());
        response.setTaskSummary(task.getTaskSummary());
        response.setTaskConfig(readJsonNode(task.getTaskConfigJson()));
        response.setSuccessNotify(task.getSuccessNotify() != null && task.getSuccessNotify() == 1);
        response.setFailureNotify(task.getFailureNotify() != null && task.getFailureNotify() == 1);
        response.setNotifyChannels(readStringList(task.getNotifyChannelsJson()));
        response.setNotifyUserIds(readLongList(task.getNotifyUserIdsJson()));
        response.setRunCount(task.getRunCount());
        response.setOperator(task.getOperator());
        response.setRemark(task.getRemark());
        response.setLastRunTime(task.getLastRunTime());
        response.setNextRunTime(task.getNextRunTime());
        response.setLastSuccessTime(task.getLastSuccessTime());
        response.setLastFailureTime(task.getLastFailureTime());
        response.setLastErrorMessage(task.getLastErrorMessage());
        response.setCreateTime(task.getCreateTime());
        return response;
    }

    private void markTaskPausedOnRestoreFailure(Long taskId, String message) {
        SysScheduledTask task = requireTask(taskId);
        task.setStatus(STATUS_PAUSED);
        task.setNextRunTime(null);
        task.setLastErrorMessage(message);
        sysScheduledTaskMapper.updateById(task);
    }

    private String buildRootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return StringUtils.hasText(current.getMessage()) ? current.getMessage() : throwable.getClass().getSimpleName();
    }

    private static class RuntimeTask {
        private final Long taskId;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final ScheduledFuture<?> future;

        private RuntimeTask(Long taskId) {
            this(taskId, null);
        }

        private RuntimeTask(Long taskId, ScheduledFuture<?> future) {
            this.taskId = taskId;
            this.future = future;
        }
    }
}
