package com.sak.service.service;

import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.NotificationSendResponse;

public interface NotificationDispatchService {

    NotificationSendResponse dispatch(String senderName, NotificationSendRequest request);
}
