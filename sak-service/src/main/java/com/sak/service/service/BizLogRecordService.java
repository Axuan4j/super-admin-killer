package com.sak.service.service;

import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.service.ILogRecordService;
import com.sak.service.entity.SysOperLog;
import com.sak.service.mapper.SysOperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.lang.reflect.Field;

@Component
@RequiredArgsConstructor
public class BizLogRecordService implements ILogRecordService {

    private final SysOperLogMapper sysOperLogMapper;

    @Override
    public void record(LogRecord logRecord) {
        SysOperLog operLog = new SysOperLog();
        operLog.setBizNo(logRecord.getBizNo());
        operLog.setLogType(logRecord.getType());
        operLog.setSubType(logRecord.getSubType());
        operLog.setOperator(logRecord.getOperator());
        operLog.setAction(logRecord.getAction());
        operLog.setExtra(logRecord.getExtra());
        operLog.setSuccess(logRecord.isFail() ? 0 : 1);
        operLog.setErrMsg(readField(logRecord, "fail"));

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operLog.setIp(resolveClientIp(request));
            operLog.setMethod(logRecord.getType());
            operLog.setRequestUrl(request.getRequestURI());
            operLog.setRequestMethod(request.getMethod());
        }
        sysOperLogMapper.insert(operLog);
    }

    @Override
    public List<LogRecord> queryLog(String bizNo, String type) {
        return Collections.emptyList();
    }

    @Override
    public List<LogRecord> queryLogByBizNo(String bizNo, String type, String subType) {
        return Collections.emptyList();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String readField(LogRecord logRecord, String fieldName) {
        try {
            Field field = LogRecord.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(logRecord);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
