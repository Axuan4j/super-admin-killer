package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.dto.NotificationRecordDetailResponse;
import com.sak.service.dto.NotificationRecordQueryRequest;
import com.sak.service.dto.NotificationRecordResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.SysNotificationRecord;
import com.sak.service.mapper.SysNotificationRecordMapper;
import com.sak.service.service.NotificationRecordService;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationRecordServiceImpl implements NotificationRecordService {

    private static final Map<String, String> SORT_FIELDS = new LinkedHashMap<>();

    static {
        SORT_FIELDS.put("id", "id");
        SORT_FIELDS.put("title", "title");
        SORT_FIELDS.put("senderName", "sender_name");
        SORT_FIELDS.put("status", "status");
        SORT_FIELDS.put("createTime", "create_time");
    }

    private final SysNotificationRecordMapper sysNotificationRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void saveRecord(NotificationRecordDetailResponse record) {
        SysNotificationRecord entity = new SysNotificationRecord();
        entity.setSenderName(record.getSenderName());
        entity.setTitle(record.getTitle());
        entity.setContent(record.getContent());
        entity.setChannelsJson(writeJson(record.getChannels()));
        entity.setSendAll(Boolean.TRUE.equals(record.getSendAll()) ? 1 : 0);
        entity.setRecipientCount(record.getRecipientCount());
        entity.setSuccessUserCount(record.getSuccessUserCount());
        entity.setStatus(record.getStatus());
        entity.setChannelSuccessCountsJson(writeJson(record.getChannelSuccessCounts()));
        entity.setChannelSkipCountsJson(writeJson(record.getChannelSkipCounts()));
        entity.setRecipientDetailsJson(writeJson(record.getRecipientDetails()));
        sysNotificationRecordMapper.insert(entity);
        record.setId(entity.getId());
    }

    @Override
    public PageResponse<NotificationRecordResponse> listRecords(NotificationRecordQueryRequest request) {
        LambdaQueryWrapper<SysNotificationRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getTitle())) {
            queryWrapper.like(SysNotificationRecord::getTitle, request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getSenderName())) {
            queryWrapper.like(SysNotificationRecord::getSenderName, request.getSenderName().trim());
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(SysNotificationRecord::getStatus, request.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getChannelCode())) {
            queryWrapper.like(SysNotificationRecord::getChannelsJson, request.getChannelCode().trim().toUpperCase());
        }
        Page<SysNotificationRecord> page = sysNotificationRecordMapper.selectPage(
                PageUtils.buildPage(request, SORT_FIELDS, "id", "desc"),
                queryWrapper
        );
        List<NotificationRecordResponse> records = page.getRecords().stream().map(this::toResponse).toList();
        return PageUtils.toResponse(page, records, request);
    }

    @Override
    public NotificationRecordDetailResponse getRecordDetail(Long id) {
        SysNotificationRecord entity = sysNotificationRecordMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("发送记录不存在");
        }
        NotificationRecordDetailResponse response = new NotificationRecordDetailResponse();
        fillCommonFields(entity, response);
        response.setRecipientDetails(readJsonList(
                entity.getRecipientDetailsJson(),
                new TypeReference<List<NotificationRecordDetailResponse.RecipientDetail>>() {}
        ));
        return response;
    }

    private NotificationRecordResponse toResponse(SysNotificationRecord entity) {
        NotificationRecordResponse response = new NotificationRecordResponse();
        fillCommonFields(entity, response);
        return response;
    }

    private void fillCommonFields(SysNotificationRecord entity, NotificationRecordResponse response) {
        response.setId(entity.getId());
        response.setSenderName(entity.getSenderName());
        response.setTitle(entity.getTitle());
        response.setContent(entity.getContent());
        response.setChannels(readJsonList(entity.getChannelsJson(), new TypeReference<List<String>>() {}));
        response.setSendAll(entity.getSendAll() != null && entity.getSendAll() == 1);
        response.setRecipientCount(entity.getRecipientCount());
        response.setSuccessUserCount(entity.getSuccessUserCount());
        response.setStatus(entity.getStatus());
        response.setChannelSuccessCounts(readJsonMap(entity.getChannelSuccessCountsJson()));
        response.setChannelSkipCounts(readJsonMap(entity.getChannelSkipCountsJson()));
        response.setCreateTime(entity.getCreateTime());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化通知记录失败", ex);
        }
    }

    private <T> List<T> readJsonList(String json, TypeReference<List<T>> typeReference) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析通知记录失败", ex);
        }
    }

    private Map<String, Integer> readJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析通知记录失败", ex);
        }
    }
}
