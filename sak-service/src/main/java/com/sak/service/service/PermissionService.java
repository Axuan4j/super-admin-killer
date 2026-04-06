package com.sak.service.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> permission.equals(authority.getAuthority()));
    }

    public void requirePermission(Authentication authentication, String permission) {
        if (!hasPermission(authentication, permission)) {
            throw new IllegalStateException("权限不足");
        }
    }
}
