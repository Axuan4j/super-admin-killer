package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.NotificationChannelSendResult;
import com.sak.service.dto.NotificationDispatchMessage;
import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.NotificationSendResponse;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.AdminNotificationService;
import com.sak.service.service.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final SysUserMapper sysUserMapper;
    private final List<NotificationChannel> notificationChannels;

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
    @LogRecord(success = "发送通知：{{#p1.title}}", fail = "发送通知失败：{{#p1.title}}", type = "NOTIFICATION", subType = "SEND", bizNo = "{{#p1.title}}")
    public NotificationSendResponse sendNotification(Authentication authentication, NotificationSendRequest request) {
        validateRequest(request);

        List<SysUser> recipients = resolveRecipients(request);
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("没有可发送的目标用户");
        }

        String senderName = resolveSenderName(authentication);
        NotificationDispatchMessage message = new NotificationDispatchMessage(request.getTitle().trim(), request.getContent().trim(), senderName);
        Map<String, NotificationChannel> channelMap = notificationChannels.stream()
                .collect(Collectors.toMap(channel -> channel.getChannelCode().toUpperCase(Locale.ROOT), Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<NotificationChannel> selectedChannels = request.getChannels().stream()
                .map(channelCode -> normalizeChannel(channelCode, channelMap))
                .distinct()
                .map(channelMap::get)
                .toList();

        selectedChannels.forEach(NotificationChannel::validateGlobalConfig);

        NotificationSendResponse response = new NotificationSendResponse();
        response.setRecipientCount(recipients.size());

        Set<Long> successUserIds = new LinkedHashSet<>();
        for (SysUser recipient : recipients) {
            for (NotificationChannel channel : selectedChannels) {
                NotificationChannelSendResult result = channel.send(message, recipient);
                if (result.isSuccess()) {
                    successUserIds.add(recipient.getId());
                    response.getChannelSuccessCounts().merge(channel.getChannelCode(), 1, Integer::sum);
                    continue;
                }
                if (result.isSkipped()) {
                    response.getChannelSkipCounts().merge(channel.getChannelCode(), 1, Integer::sum);
                }
            }
        }
        response.setSuccessUserCount(successUserIds.size());
        selectedChannels.forEach(channel -> {
            response.getChannelSuccessCounts().putIfAbsent(channel.getChannelCode(), 0);
            response.getChannelSkipCounts().putIfAbsent(channel.getChannelCode(), 0);
        });
        return response;
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
        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一种推送渠道");
        }
        boolean sendAll = Boolean.TRUE.equals(request.getSendAll());
        if (!sendAll && (request.getUserIds() == null || request.getUserIds().isEmpty())) {
            throw new IllegalArgumentException("请选择接收用户");
        }
    }

    private List<SysUser> resolveRecipients(NotificationSendRequest request) {
        if (Boolean.TRUE.equals(request.getSendAll())) {
            return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .select(SysUser::getId, SysUser::getUsername, SysUser::getNickName, SysUser::getEmail, SysUser::getWxPusherUid, SysUser::getStatus)
                            .eq(SysUser::getStatus, "0")
                            .orderByAsc(SysUser::getId));
        }

        Set<Long> distinctIds = new LinkedHashSet<>(request.getUserIds());
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getNickName, SysUser::getEmail, SysUser::getWxPusherUid, SysUser::getStatus)
                        .in(SysUser::getId, distinctIds)
                        .eq(SysUser::getStatus, "0")
                        .orderByAsc(SysUser::getId));
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

    private String normalizeChannel(String channelCode, Map<String, NotificationChannel> channelMap) {
        if (!StringUtils.hasText(channelCode)) {
            throw new IllegalArgumentException("存在无效的推送渠道");
        }
        String normalized = channelCode.trim().toUpperCase(Locale.ROOT);
        if (!channelMap.containsKey(normalized)) {
            throw new IllegalArgumentException("不支持的推送渠道：" + channelCode);
        }
        return normalized;
    }
}
