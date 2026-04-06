package com.superkiller.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.superkiller.backend.dto.OperLogResponse;
import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.entity.SysOperLog;
import com.superkiller.backend.mapper.SysOperLogMapper;
import com.superkiller.backend.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final SysOperLogMapper sysOperLogMapper;

    @Override
    public PageResponse<OperLogResponse> listLogs(String keyword, Integer success, long current, long size) {
        LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getId);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysOperLog::getOperator, keyword)
                    .or()
                    .like(SysOperLog::getAction, keyword)
                    .or()
                    .like(SysOperLog::getLogType, keyword)
                    .or()
                    .like(SysOperLog::getRequestUrl, keyword));
        }
        if (success != null) {
            queryWrapper.eq(SysOperLog::getSuccess, success);
        }

        Page<SysOperLog> page = sysOperLogMapper.selectPage(new Page<>(current, size), queryWrapper);
        return new PageResponse<>(
                page.getRecords().stream().map(this::toResponse).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private OperLogResponse toResponse(SysOperLog log) {
        OperLogResponse response = new OperLogResponse();
        response.setId(log.getId());
        response.setBizNo(log.getBizNo());
        response.setLogType(log.getLogType());
        response.setSubType(log.getSubType());
        response.setOperator(log.getOperator());
        response.setAction(log.getAction());
        response.setExtra(log.getExtra());
        response.setIp(log.getIp());
        response.setMethod(log.getMethod());
        response.setRequestUrl(log.getRequestUrl());
        response.setRequestMethod(log.getRequestMethod());
        response.setExecutionTime(log.getExecutionTime());
        response.setSuccess(log.getSuccess());
        response.setErrMsg(log.getErrMsg());
        response.setCreateTime(log.getCreateTime());
        return response;
    }
}
