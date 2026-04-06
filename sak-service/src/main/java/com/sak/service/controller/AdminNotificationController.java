package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping("/recipient-options")
    @PreAuthorize("hasAuthority('system:notification:view')")
    public Result<List<NotificationRecipientResponse>> listRecipientOptions() {
        return Result.success(adminNotificationService.listRecipients());
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('system:notification:send')")
    public Result<Map<String, Integer>> sendNotification(Authentication authentication, @RequestBody NotificationSendRequest request) {
        int successCount = adminNotificationService.sendNotification(authentication, request);
        return Result.success(Map.of("successCount", successCount));
    }
}
