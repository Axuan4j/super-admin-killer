package com.sak.service.service;

import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationRecordDetailResponse;
import com.sak.service.dto.NotificationRecordQueryRequest;
import com.sak.service.dto.NotificationRecordResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.NotificationSendResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AdminNotificationService {
    List<NotificationRecipientResponse> listRecipients();

    PageResponse<NotificationRecordResponse> listRecords(NotificationRecordQueryRequest request);

    NotificationRecordDetailResponse getRecordDetail(Long id);

    NotificationSendResponse sendNotification(Authentication authentication, NotificationSendRequest request);
}
