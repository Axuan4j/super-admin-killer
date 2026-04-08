package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sak.service.dto.OperLogQueryRequest;
import com.sak.service.dto.OperLogResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.SysOperLog;
import com.sak.service.mapper.SysOperLogMapper;
import com.sak.service.service.OperLogService;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private static final Map<String, String> OPER_LOG_SORT_FIELDS = new LinkedHashMap<>();

    static {
        OPER_LOG_SORT_FIELDS.put("id", "id");
        OPER_LOG_SORT_FIELDS.put("operator", "operator");
        OPER_LOG_SORT_FIELDS.put("logType", "log_type");
        OPER_LOG_SORT_FIELDS.put("success", "success");
        OPER_LOG_SORT_FIELDS.put("createTime", "create_time");
    }

    private final SysOperLogMapper sysOperLogMapper;

    @Override
    public PageResponse<OperLogResponse> listLogs(OperLogQueryRequest request) {
        LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getOperator())) {
            queryWrapper.like(SysOperLog::getOperator, request.getOperator());
        }
        if (StringUtils.hasText(request.getLogType())) {
            queryWrapper.like(SysOperLog::getLogType, request.getLogType());
        }
        if (StringUtils.hasText(request.getAction())) {
            queryWrapper.like(SysOperLog::getAction, request.getAction());
        }
        if (request.getSuccess() != null) {
            queryWrapper.eq(SysOperLog::getSuccess, request.getSuccess());
        }

        Page<SysOperLog> page = sysOperLogMapper.selectPage(PageUtils.buildPage(request, OPER_LOG_SORT_FIELDS, "id", "desc"), queryWrapper);
        return PageUtils.toResponse(page, page.getRecords().stream().map(this::toResponse).toList(), request);
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
