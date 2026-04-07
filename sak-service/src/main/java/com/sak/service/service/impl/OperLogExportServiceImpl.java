package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.config.StaticResourceConfig;
import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.OperLogExportRequest;
import com.sak.service.entity.SysExportRecord;
import com.sak.service.entity.SysOperLog;
import com.sak.service.mapper.SysOperLogMapper;
import com.sak.service.service.AbstractAsyncExportService;
import com.sak.service.service.ExportAsyncInvoker;
import com.sak.service.service.ExportRecordService;
import com.sak.service.service.OperLogExportService;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OperLogExportServiceImpl extends AbstractAsyncExportService<OperLogExportRequest, SysOperLog> implements OperLogExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOperLogMapper sysOperLogMapper;
    private final ExportRecordService exportRecordService;

    public OperLogExportServiceImpl(
            ExportRecordService exportRecordService,
            StaticResourceConfig staticResourceConfig,
            ObjectMapper objectMapper,
            ExportAsyncInvoker exportAsyncInvoker,
            SysOperLogMapper sysOperLogMapper
    ) {
        super(exportRecordService, staticResourceConfig, objectMapper, exportAsyncInvoker);
        this.sysOperLogMapper = sysOperLogMapper;
        this.exportRecordService = exportRecordService;
    }

    @Override
    public ExportRecordResponse createExportTask(OperLogExportRequest request, String operator) {
        SysExportRecord record = submitExport(request, operator);
        ExportRecordResponse response = new ExportRecordResponse();
        response.setId(record.getId());
        response.setBizType(record.getBizType());
        response.setFileName(record.getFileName());
        response.setQueryCondition(record.getQueryCondition());
        response.setStatus(record.getStatus());
        response.setOperator(record.getOperator());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    @Override
    protected String getBizType() {
        return "OPER_LOG";
    }

    @Override
    protected List<String> buildHeaderColumns() {
        return List.of("ID", "业务标识", "日志类型", "子类型", "操作人", "操作描述", "附加信息", "IP地址", "方法标识", "请求地址", "请求方式", "执行耗时(ms)", "结果", "错误信息", "创建时间");
    }

    @Override
    protected List<String> mapRow(SysOperLog item) {
        return List.of(
                String.valueOf(item.getId()),
                defaultString(item.getBizNo()),
                defaultString(item.getLogType()),
                defaultString(item.getSubType()),
                defaultString(item.getOperator()),
                defaultString(item.getAction()),
                defaultString(item.getExtra()),
                defaultString(item.getIp()),
                defaultString(item.getMethod()),
                defaultString(item.getRequestUrl()),
                defaultString(item.getRequestMethod()),
                item.getExecutionTime() == null ? "" : String.valueOf(item.getExecutionTime()),
                item.getSuccess() != null && item.getSuccess() == 1 ? "成功" : "失败",
                defaultString(item.getErrMsg()),
                item.getCreateTime() == null ? "" : item.getCreateTime().format(DATE_TIME_FORMATTER)
        );
    }

    @Override
    protected List<SysOperLog> fetchBatch(long lastId, int batchSize, OperLogExportRequest query) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .gt(SysOperLog::getId, lastId)
                .orderByAsc(SysOperLog::getId)
                .last("limit " + batchSize);
        if (StringUtils.hasText(query.getOperator())) {
            wrapper.like(SysOperLog::getOperator, query.getOperator().trim());
        }
        if (StringUtils.hasText(query.getLogType())) {
            wrapper.like(SysOperLog::getLogType, query.getLogType().trim());
        }
        if (StringUtils.hasText(query.getAction())) {
            wrapper.like(SysOperLog::getAction, query.getAction().trim());
        }
        if (query.getSuccess() != null) {
            wrapper.eq(SysOperLog::getSuccess, query.getSuccess());
        }
        return sysOperLogMapper.selectList(wrapper);
    }

    @Override
    protected long extractId(SysOperLog item) {
        return item.getId() == null ? 0L : item.getId();
    }

    @Override
    protected String buildFileName(OperLogExportRequest query) {
        return "oper-log-export-" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".zip";
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
