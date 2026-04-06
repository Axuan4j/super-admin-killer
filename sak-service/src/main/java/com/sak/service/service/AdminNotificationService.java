package com.sak.service.service;

import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationSendRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AdminNotificationService {
    List<NotificationRecipientResponse> listRecipients();

    int sendNotification(Authentication authentication, NotificationSendRequest request);
}
