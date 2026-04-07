package com.sak.service.service;

import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.NotificationSendResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AdminNotificationService {
    List<NotificationRecipientResponse> listRecipients();

    NotificationSendResponse sendNotification(Authentication authentication, NotificationSendRequest request);
}
