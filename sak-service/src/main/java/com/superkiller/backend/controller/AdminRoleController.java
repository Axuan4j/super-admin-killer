package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.RoleOptionResponse;
import com.superkiller.backend.dto.RoleSaveRequest;
import com.superkiller.backend.service.AdminRoleService;
import com.superkiller.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<PageResponse<RoleOptionResponse>> listRoles(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        permissionService.requirePermission(authentication, "system:role:view");
        return Result.success(adminRoleService.listRoles(keyword, status, current, size));
    }

    @PostMapping
    public Result<RoleOptionResponse> createRole(Authentication authentication, @RequestBody RoleSaveRequest request) {
        permissionService.requirePermission(authentication, "system:role:add");
        return Result.success(adminRoleService.createRole(request));
    }

    @PutMapping("/{id}")
    public Result<RoleOptionResponse> updateRole(Authentication authentication, @PathVariable Long id, @RequestBody RoleSaveRequest request) {
        permissionService.requirePermission(authentication, "system:role:edit");
        return Result.success(adminRoleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(Authentication authentication, @PathVariable Long id) {
        permissionService.requirePermission(authentication, "system:role:remove");
        adminRoleService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/{id}/menu-ids")
    public Result<List<Long>> getRoleMenuIds(Authentication authentication, @PathVariable Long id) {
        permissionService.requirePermission(authentication, "system:role:edit");
        return Result.success(adminRoleService.getRoleMenuIds(id));
    }

    @PutMapping("/{id}/menu-ids")
    public Result<Void> updateRoleMenuIds(Authentication authentication, @PathVariable Long id, @RequestBody List<Long> menuIds) {
        permissionService.requirePermission(authentication, "system:role:edit");
        adminRoleService.updateRoleMenus(id, menuIds);
        return Result.success();
    }
}
