package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.OnlineUserSessionResponse;
import com.sak.service.service.AdminOnlineUserService;
import com.sak.service.service.PermissionService;
import com.sak.service.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/online-users")
@RequiredArgsConstructor
public class AdminOnlineUserController {

    private final AdminOnlineUserService adminOnlineUserService;
    private final PermissionService permissionService;
    private final TokenService tokenService;

    @GetMapping
    public Result<List<OnlineUserSessionResponse>> listOnlineUsers(Authentication authentication, HttpServletRequest request) {
        permissionService.requirePermission(authentication, "system:online:view");
        return Result.success(adminOnlineUserService.listOnlineUsers(resolveCurrentSessionId(request)));
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> forceLogout(Authentication authentication, @PathVariable String sessionId) {
        permissionService.requirePermission(authentication, "system:online:forceLogout");
        adminOnlineUserService.forceLogout(sessionId);
        return Result.success();
    }

    private String resolveCurrentSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return tokenService.getSessionIdByAccessToken(authHeader.substring(7));
    }
}
