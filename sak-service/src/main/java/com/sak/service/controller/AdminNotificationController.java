package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.NotificationRecipientResponse;
import com.sak.service.dto.NotificationSendResponse;
import com.sak.service.convert.RequestVoConverter;
import com.sak.service.service.AdminNotificationService;
import com.sak.service.vo.NotificationSendVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/system/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;
    private final RequestVoConverter requestVoMapper;

    @GetMapping("/recipient-options")
    @PreAuthorize("hasAuthority('system:notification:view')")
    public Result<List<NotificationRecipientResponse>> listRecipientOptions() {
        return Result.success(adminNotificationService.listRecipients());
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('system:notification:send')")
    public Result<NotificationSendResponse> sendNotification(Authentication authentication, @Valid @RequestBody NotificationSendVO request) {
        return Result.success(adminNotificationService.sendNotification(authentication, requestVoMapper.toNotificationSendRequest(request)));
    }
}
