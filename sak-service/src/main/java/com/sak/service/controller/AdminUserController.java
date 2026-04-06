package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.RoleOptionResponse;
import com.sak.service.dto.UserAdminResponse;
import com.sak.service.dto.UserSaveRequest;
import com.sak.service.service.AdminUserService;
import com.sak.service.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<PageResponse<UserAdminResponse>> listUsers(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        permissionService.requirePermission(authentication, "system:user:view");
        return Result.success(adminUserService.listUsers(keyword, status, current, size));
    }

    @GetMapping("/role-options")
    public Result<List<RoleOptionResponse>> listRoleOptions(Authentication authentication) {
        permissionService.requirePermission(authentication, "system:user:view");
        return Result.success(adminUserService.listRoleOptions());
    }

    @PostMapping
    public Result<UserAdminResponse> createUser(Authentication authentication, @RequestBody UserSaveRequest request) {
        permissionService.requirePermission(authentication, "system:user:add");
        return Result.success(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<UserAdminResponse> updateUser(Authentication authentication, @PathVariable Long id, @RequestBody UserSaveRequest request) {
        permissionService.requirePermission(authentication, "system:user:edit");
        return Result.success(adminUserService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(Authentication authentication, @PathVariable Long id) {
        permissionService.requirePermission(authentication, "system:user:remove");
        adminUserService.deleteUser(id);
        return Result.success();
    }
}
