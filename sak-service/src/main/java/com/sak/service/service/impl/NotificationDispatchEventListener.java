package com.sak.service.service.impl;

import com.sak.service.dto.NotificationChannelSendResult;
import com.sak.service.dto.NotificationDispatchEvent;
import com.sak.service.dto.NotificationRecordDetailResponse;
import com.sak.service.entity.SysUser;
import com.sak.service.service.NotificationChannel;
import com.sak.service.service.NotificationRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchEventListener {

    private final List<NotificationChannel> notificationChannels;
    private final NotificationRecordService notificationRecordService;

    @Async("notificationTaskExecutor")
    @org.springframework.context.event.EventListener
    public void handle(NotificationDispatchEvent event) {
        Map<String, NotificationChannel> channelMap = notificationChannels.stream()
                .collect(Collectors.toMap(
                        channel -> channel.getChannelCode().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<NotificationChannel> selectedChannels = event.channels().stream()
                .map(channelMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        NotificationRecordDetailResponse record = new NotificationRecordDetailResponse();
        record.setId(event.recordId());
        record.setSenderName(event.message().getSenderName());
        record.setTitle(event.message().getTitle());
        record.setContent(event.message().getContent());
        record.setChannels(event.channels());
        record.setSendAll(event.sendAll());
        record.setRecipientCount(event.recipients().size());

        Set<Long> successUserIds = new LinkedHashSet<>();
        try {
            for (SysUser recipient : event.recipients()) {
                NotificationRecordDetailResponse.RecipientDetail recipientDetail = new NotificationRecordDetailResponse.RecipientDetail();
                recipientDetail.setUserId(recipient.getId());
                recipientDetail.setUsername(recipient.getUsername());
                recipientDetail.setNickName(recipient.getNickName());

                boolean recipientSuccess = false;
                for (NotificationChannel channel : selectedChannels) {
                    NotificationChannelSendResult result;
                    try {
                        result = channel.send(event.message(), recipient);
                    } catch (Exception ex) {
                        log.warn("Notification channel send failed, recordId={}, channel={}, userId={}", event.recordId(), channel.getChannelCode(), recipient.getId(), ex);
                        result = NotificationChannelSendResult.failed(ex.getMessage());
                    }

                    NotificationRecordDetailResponse.ChannelDetail channelDetail = new NotificationRecordDetailResponse.ChannelDetail();
                    channelDetail.setChannelCode(channel.getChannelCode());
                    if (result.isSuccess()) {
                        channelDetail.setStatus("SUCCESS");
                        successUserIds.add(recipient.getId());
                        recipientSuccess = true;
                        record.getChannelSuccessCounts().merge(channel.getChannelCode(), 1, Integer::sum);
                    } else if (result.isSkipped()) {
                        channelDetail.setStatus("SKIPPED");
                        record.getChannelSkipCounts().merge(channel.getChannelCode(), 1, Integer::sum);
                    } else {
                        channelDetail.setStatus("FAILED");
                    }
                    channelDetail.setMessage(result.getMessage());
                    recipientDetail.getChannels().add(channelDetail);
                }
                recipientDetail.setStatus(recipientSuccess ? "SUCCESS" : "FAILED");
                record.getRecipientDetails().add(recipientDetail);
            }

            record.setSuccessUserCount(successUserIds.size());
            selectedChannels.forEach(channel -> {
                record.getChannelSuccessCounts().putIfAbsent(channel.getChannelCode(), 0);
                record.getChannelSkipCounts().putIfAbsent(channel.getChannelCode(), 0);
            });
            if (successUserIds.size() == event.recipients().size()) {
                record.setStatus("SUCCESS");
            } else if (successUserIds.isEmpty()) {
                record.setStatus("FAILED");
            } else {
                record.setStatus("PARTIAL");
            }
        } catch (Exception ex) {
            log.error("Notification async dispatch failed, recordId={}", event.recordId(), ex);
            record.setSuccessUserCount(successUserIds.size());
            record.setStatus(successUserIds.isEmpty() ? "FAILED" : "PARTIAL");
        }

        notificationRecordService.updateRecord(record);
    }
}
