package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.dto.NotificationRecipientResponse;
import com.superkiller.backend.dto.NotificationSendRequest;
import com.superkiller.backend.service.AdminNotificationService;
import com.superkiller.backend.service.PermissionService;
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
