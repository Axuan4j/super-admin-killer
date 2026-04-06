package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.RoleOptionResponse;
import com.superkiller.backend.dto.UserAdminResponse;
import com.superkiller.backend.dto.UserSaveRequest;
import com.superkiller.backend.service.AdminUserService;
import com.superkiller.backend.service.PermissionService;
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
