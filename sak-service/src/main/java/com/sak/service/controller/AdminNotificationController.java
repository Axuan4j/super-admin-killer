package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.service.AdminNotificationService;
import com.sak.service.service.PermissionService;
import lombok.RequiredArgsConstructor;
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
    private final PermissionService permissionService;

    @GetMapping("/recipient-options")
    public Result<List<NotificationRecipientResponse>> listRecipientOptions(Authentication authentication) {
        permissionService.requirePermission(authentication, "system:notification:view");
        return Result.success(adminNotificationService.listRecipients());
    }

    @PostMapping("/send")
    public Result<Map<String, Integer>> sendNotification(Authentication authentication, @RequestBody NotificationSendRequest request) {
        permissionService.requirePermission(authentication, "system:notification:send");
        int successCount = adminNotificationService.sendNotification(authentication, request);
        return Result.success(Map.of("successCount", successCount));
    }
}
