package com.superkiller.backend.service;

import com.superkiller.backend.dto.NotificationRecipientResponse;
import com.superkiller.backend.dto.NotificationSendRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AdminNotificationService {
    List<NotificationRecipientResponse> listRecipients();

    int sendNotification(Authentication authentication, NotificationSendRequest request);
}
