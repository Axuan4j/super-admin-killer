package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sak.service.dto.NotificationChannelSendResult;
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

        NotificationSendResponse response = new NotificationSendResponse();
        response.setRecipientCount(recipients.size());
        NotificationRecordDetailResponse record = new NotificationRecordDetailResponse();
        record.setSenderName(message.getSenderName());
        record.setTitle(message.getTitle());
        record.setContent(message.getContent());
        record.setChannels(selectedChannels.stream().map(NotificationChannel::getChannelCode).toList());
        record.setSendAll(Boolean.TRUE.equals(request.getSendAll()));
        record.setRecipientCount(recipients.size());

        Set<Long> successUserIds = new LinkedHashSet<>();
        for (SysUser recipient : recipients) {
            NotificationRecordDetailResponse.RecipientDetail recipientDetail = new NotificationRecordDetailResponse.RecipientDetail();
            recipientDetail.setUserId(recipient.getId());
            recipientDetail.setUsername(recipient.getUsername());
            recipientDetail.setNickName(recipient.getNickName());

            boolean recipientSuccess = false;
            for (NotificationChannel channel : selectedChannels) {
                NotificationChannelSendResult result;
                try {
                    result = channel.send(message, recipient);
                } catch (Exception ex) {
                    result = NotificationChannelSendResult.failed(ex.getMessage());
                }

                NotificationRecordDetailResponse.ChannelDetail channelDetail = new NotificationRecordDetailResponse.ChannelDetail();
                channelDetail.setChannelCode(channel.getChannelCode());
                if (result.isSuccess()) {
                    channelDetail.setStatus("SUCCESS");
                    successUserIds.add(recipient.getId());
                    recipientSuccess = true;
                    response.getChannelSuccessCounts().merge(channel.getChannelCode(), 1, Integer::sum);
                } else if (result.isSkipped()) {
                    channelDetail.setStatus("SKIPPED");
                    response.getChannelSkipCounts().merge(channel.getChannelCode(), 1, Integer::sum);
                } else {
                    channelDetail.setStatus("FAILED");
                }
                channelDetail.setMessage(result.getMessage());
                recipientDetail.getChannels().add(channelDetail);
            }
            recipientDetail.setStatus(recipientSuccess ? "SUCCESS" : "FAILED");
            record.getRecipientDetails().add(recipientDetail);
        }
        response.setSuccessUserCount(successUserIds.size());
        selectedChannels.forEach(channel -> {
            response.getChannelSuccessCounts().putIfAbsent(channel.getChannelCode(), 0);
            response.getChannelSkipCounts().putIfAbsent(channel.getChannelCode(), 0);
        });
        record.setSuccessUserCount(successUserIds.size());
        record.setChannelSuccessCounts(new LinkedHashMap<>(response.getChannelSuccessCounts()));
        record.setChannelSkipCounts(new LinkedHashMap<>(response.getChannelSkipCounts()));
        if (successUserIds.size() == recipients.size()) {
            record.setStatus("SUCCESS");
        } else if (successUserIds.isEmpty()) {
            record.setStatus("FAILED");
        } else {
            record.setStatus("PARTIAL");
        }
        notificationRecordService.saveRecord(record);
        response.setRecordId(record.getId());
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
