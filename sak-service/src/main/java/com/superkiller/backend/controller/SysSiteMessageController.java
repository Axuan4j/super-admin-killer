package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.entity.SysSiteMessage;
import com.superkiller.backend.service.SysSiteMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/site-messages")
@RequiredArgsConstructor
public class SysSiteMessageController {

    private final SysSiteMessageService sysSiteMessageService;

    @GetMapping("/current")
    public Result<List<SysSiteMessage>> getCurrentUserMessages(Authentication authentication) {
        return Result.success(sysSiteMessageService.getCurrentUserMessages(authentication.getName()));
    }

    @GetMapping("/current/unread-count")
    public Result<Map<String, Long>> getCurrentUserUnreadCount(Authentication authentication) {
        long unreadCount = sysSiteMessageService.getCurrentUserUnreadCount(authentication.getName());
        return Result.success(Map.of("unreadCount", unreadCount));
    }

    @PostMapping("/current/mark-read")
    public Result<Map<String, Integer>> markCurrentUserMessagesRead(Authentication authentication) {
        int updatedCount = sysSiteMessageService.markCurrentUserMessagesRead(authentication.getName());
        return Result.success(Map.of("updatedCount", updatedCount));
    }
}
