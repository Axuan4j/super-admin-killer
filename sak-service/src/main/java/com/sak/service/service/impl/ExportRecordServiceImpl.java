package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sak.service.config.StaticResourceConfig;
import com.sak.service.dto.ExportRecordResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.SysExportRecord;
import com.sak.service.mapper.SysExportRecordMapper;
import com.sak.service.service.ExportRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExportRecordServiceImpl implements ExportRecordService {

    private final SysExportRecordMapper sysExportRecordMapper;
    private final StaticResourceConfig staticResourceConfig;

    @Override
    public SysExportRecord createPendingRecord(String bizType, String fileName, String queryCondition, String operator) {
        SysExportRecord record = new SysExportRecord();
        LocalDateTime now = LocalDateTime.now();
        record.setBizType(bizType);
        record.setFileName(fileName);
        record.setQueryCondition(queryCondition);
        record.setStatus("PENDING");
        record.setOperator(operator);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        sysExportRecordMapper.insert(record);
        return record;
    }

    @Override
    public void markRunning(Long id) {
        SysExportRecord record = requireRecord(id);
        record.setStatus("RUNNING");
        record.setErrMsg(null);
        sysExportRecordMapper.updateById(record);
    }

    @Override
    public void markSuccess(Long id, String filePath, long fileSize, long totalCount) {
        SysExportRecord record = requireRecord(id);
        record.setStatus("SUCCESS");
        record.setFilePath(filePath);
        record.setFileSize(fileSize);
        record.setTotalCount(totalCount);
        record.setErrMsg(null);
        record.setFinishTime(LocalDateTime.now());
        sysExportRecordMapper.updateById(record);
    }

    @Override
    public void markFailed(Long id, String errMsg) {
        SysExportRecord record = requireRecord(id);
        record.setStatus("FAILED");
        record.setErrMsg(errMsg);
        record.setFinishTime(LocalDateTime.now());
        sysExportRecordMapper.updateById(record);
    }

    @Override
    public PageResponse<ExportRecordResponse> listRecords(String bizType, String status, long current, long size) {
        LambdaQueryWrapper<SysExportRecord> wrapper = new LambdaQueryWrapper<SysExportRecord>()
                .orderByDesc(SysExportRecord::getId);
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(SysExportRecord::getBizType, bizType.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysExportRecord::getStatus, status.trim());
        }
        Page<SysExportRecord> page = sysExportRecordMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResponse<>(
                page.getRecords().stream().map(this::toResponse).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private ExportRecordResponse toResponse(SysExportRecord record) {
        ExportRecordResponse response = new ExportRecordResponse();
        response.setId(record.getId());
        response.setBizType(record.getBizType());
        response.setFileName(record.getFileName());
        response.setQueryCondition(record.getQueryCondition());
        response.setStatus(record.getStatus());
        response.setFilePath(record.getFilePath());
        response.setDownloadUrl(record.getFilePath() == null ? null : staticResourceConfig.getStorageAccessPrefix() + normalizeRelativePath(record.getFilePath()));
        response.setFileSize(record.getFileSize());
        response.setTotalCount(record.getTotalCount());
        response.setOperator(record.getOperator());
        response.setErrMsg(record.getErrMsg());
        response.setFinishTime(record.getFinishTime());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    private SysExportRecord requireRecord(Long id) {
        SysExportRecord record = sysExportRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("导出记录不存在");
        }
        return record;
    }

    private String normalizeRelativePath(String filePath) {
        return filePath.replace("\\", "/").replaceAll("^/+", "");
    }
}
