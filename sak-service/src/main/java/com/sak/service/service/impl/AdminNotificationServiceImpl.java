package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.AdminNotificationService;
import com.sak.service.service.SysSiteMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final SysUserMapper sysUserMapper;
    private final SysSiteMessageService sysSiteMessageService;

    @Override
    public List<NotificationRecipientResponse> listRecipients() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getNickName, SysUser::getEmail, SysUser::getStatus)
                        .eq(SysUser::getStatus, "0")
                        .orderByAsc(SysUser::getId))
                .stream()
                .map(this::toRecipient)
                .toList();
    }

    @Override
    @Transactional
    @LogRecord(success = "发送通知：{{#request.title}}", fail = "发送通知失败：{{#request.title}}", type = "NOTIFICATION", subType = "SEND", bizNo = "{{#request.title}}")
    public int sendNotification(Authentication authentication, NotificationSendRequest request) {
        validateRequest(request);

        List<Long> targetUserIds = resolveTargetUserIds(request);
        if (targetUserIds.isEmpty()) {
            throw new IllegalArgumentException("没有可发送的目标用户");
        }

        String senderName = resolveSenderName(authentication);
        for (Long userId : targetUserIds) {
            sysSiteMessageService.sendMessageToUser(userId, request.getTitle().trim(), request.getContent().trim(), senderName);
        }
        return targetUserIds.size();
    }

    private void validateRequest(NotificationSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("通知标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("通知内容不能为空");
        }
        boolean sendAll = Boolean.TRUE.equals(request.getSendAll());
        if (!sendAll && (request.getUserIds() == null || request.getUserIds().isEmpty())) {
            throw new IllegalArgumentException("请选择接收用户");
        }
    }

    private List<Long> resolveTargetUserIds(NotificationSendRequest request) {
        if (Boolean.TRUE.equals(request.getSendAll())) {
            return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .select(SysUser::getId)
                            .eq(SysUser::getStatus, "0")
                            .orderByAsc(SysUser::getId))
                    .stream()
                    .map(SysUser::getId)
                    .toList();
        }

        Set<Long> distinctIds = new LinkedHashSet<>(request.getUserIds());
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .in(SysUser::getId, distinctIds)
                        .eq(SysUser::getStatus, "0")
                        .orderByAsc(SysUser::getId))
                .stream()
                .map(SysUser::getId)
                .toList();
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
        response.setStatus(user.getStatus());
        return response;
    }
}
