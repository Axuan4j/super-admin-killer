package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sak.service.dto.NotificationDispatchEvent;
import com.sak.service.dto.NotificationDispatchMessage;
import com.sak.service.dto.NotificationRecordDetailResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.NotificationSendResponse;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.NotificationChannel;
import com.sak.service.service.NotificationDispatchService;
import com.sak.service.service.NotificationRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final SysUserMapper sysUserMapper;
    private final List<NotificationChannel> notificationChannels;
    private final NotificationRecordService notificationRecordService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public NotificationSendResponse dispatch(String senderName, NotificationSendRequest request) {
        validateRequest(request);

        List<SysUser> recipients = resolveRecipients(request);
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("没有可发送的目标用户");
        }

        NotificationDispatchMessage message = new NotificationDispatchMessage(
                request.getTitle().trim(),
                request.getContent().trim(),
                StringUtils.hasText(senderName) ? senderName.trim() : "系统"
        );
        Map<String, NotificationChannel> channelMap = notificationChannels.stream()
                .collect(Collectors.toMap(channel -> channel.getChannelCode().toUpperCase(Locale.ROOT), Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<NotificationChannel> selectedChannels = request.getChannels().stream()
                .map(channelCode -> normalizeChannel(channelCode, channelMap))
                .distinct()
                .map(channelMap::get)
                .toList();

        selectedChannels.forEach(NotificationChannel::validateGlobalConfig);

        NotificationRecordDetailResponse record = new NotificationRecordDetailResponse();
        record.setSenderName(message.getSenderName());
        record.setTitle(message.getTitle());
        record.setContent(message.getContent());
        record.setChannels(selectedChannels.stream().map(NotificationChannel::getChannelCode).toList());
        record.setSendAll(Boolean.TRUE.equals(request.getSendAll()));
        record.setRecipientCount(recipients.size());
        for (SysUser recipient : recipients) {
            NotificationRecordDetailResponse.RecipientDetail recipientDetail = new NotificationRecordDetailResponse.RecipientDetail();
            recipientDetail.setUserId(recipient.getId());
            recipientDetail.setUsername(recipient.getUsername());
            recipientDetail.setNickName(recipient.getNickName());
            for (NotificationChannel channel : selectedChannels) {
                NotificationRecordDetailResponse.ChannelDetail channelDetail = new NotificationRecordDetailResponse.ChannelDetail();
                channelDetail.setChannelCode(channel.getChannelCode());
                channelDetail.setStatus("PENDING");
                channelDetail.setMessage("异步发送中");
                recipientDetail.getChannels().add(channelDetail);
            }
            recipientDetail.setStatus("PENDING");
            record.getRecipientDetails().add(recipientDetail);
        }
        selectedChannels.forEach(channel -> {
            record.getChannelSuccessCounts().putIfAbsent(channel.getChannelCode(), 0);
            record.getChannelSkipCounts().putIfAbsent(channel.getChannelCode(), 0);
        });
        record.setSuccessUserCount(0);
        record.setStatus("PENDING");
        notificationRecordService.saveRecord(record);

        applicationEventPublisher.publishEvent(new NotificationDispatchEvent(
                record.getId(),
                message,
                recipients,
                record.getChannels(),
                Boolean.TRUE.equals(request.getSendAll())
        ));

        NotificationSendResponse response = new NotificationSendResponse();
        response.setRecordId(record.getId());
        response.setRecipientCount(recipients.size());
        response.setSuccessUserCount(0);
        response.setChannelSuccessCounts(new LinkedHashMap<>(record.getChannelSuccessCounts()));
        response.setChannelSkipCounts(new LinkedHashMap<>(record.getChannelSkipCounts()));
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
