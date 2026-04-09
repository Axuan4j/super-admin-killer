package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationRecordDetailResponse;
import com.sak.service.dto.NotificationRecordQueryRequest;
import com.sak.service.dto.NotificationRecordResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.NotificationSendResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.AdminNotificationService;
import com.sak.service.service.NotificationDispatchService;
import com.sak.service.service.NotificationRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final SysUserMapper sysUserMapper;
    private final NotificationDispatchService notificationDispatchService;
    private final NotificationRecordService notificationRecordService;

    @Override
    public List<NotificationRecipientResponse> listRecipients() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getNickName, SysUser::getEmail, SysUser::getWxPusherUid, SysUser::getStatus)
                        .eq(SysUser::getStatus, "0")
                        .orderByAsc(SysUser::getId))
                .stream()
                .map(this::toRecipient)
                .toList();
    }

    @Override
    public PageResponse<NotificationRecordResponse> listRecords(NotificationRecordQueryRequest request) {
        return notificationRecordService.listRecords(request);
    }

    @Override
    public NotificationRecordDetailResponse getRecordDetail(Long id) {
        return notificationRecordService.getRecordDetail(id);
    }

    @Override
    @Transactional
    @LogRecord(success = "发送通知：{{#p1.title}}", fail = "发送通知失败：{{#p1.title}}", type = "NOTIFICATION", subType = "SEND", bizNo = "{{#p1.title}}")
    public NotificationSendResponse sendNotification(Authentication authentication, NotificationSendRequest request) {
        return notificationDispatchService.dispatch(resolveSenderName(authentication), request);
    }

    private String resolveSenderName(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "系统";
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, authentication.getName())
                .last("limit 1"));
        if (user == null) {
            return authentication.getName();
        }
        return StringUtils.hasText(user.getNickName()) ? user.getNickName() : user.getUsername();
    }

    private NotificationRecipientResponse toRecipient(SysUser user) {
        NotificationRecipientResponse response = new NotificationRecipientResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickName(user.getNickName());
        response.setEmail(user.getEmail());
        response.setWxPusherUid(user.getWxPusherUid());
        response.setStatus(user.getStatus());
        return response;
    }
}
