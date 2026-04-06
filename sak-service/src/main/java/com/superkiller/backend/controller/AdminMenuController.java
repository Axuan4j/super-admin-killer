package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.MenuAdminResponse;
import com.superkiller.backend.dto.MenuSaveRequest;
import com.superkiller.backend.service.AdminMenuService;
import com.superkiller.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<PageResponse<MenuAdminResponse>> listMenus(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String menuType,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "12") long size
    ) {
        permissionService.requirePermission(authentication, "system:permission:view");
        return Result.success(adminMenuService.listMenus(keyword, menuType, current, size));
    }

    @PostMapping
    public Result<MenuAdminResponse> createMenu(Authentication authentication, @RequestBody MenuSaveRequest request) {
        permissionService.requirePermission(authentication, "system:permission:add");
        return Result.success(adminMenuService.createMenu(request));
    }

    @PutMapping("/{id}")
    public Result<MenuAdminResponse> updateMenu(Authentication authentication, @PathVariable Long id, @RequestBody MenuSaveRequest request) {
        permissionService.requirePermission(authentication, "system:permission:edit");
        return Result.success(adminMenuService.updateMenu(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMenu(Authentication authentication, @PathVariable Long id) {
        permissionService.requirePermission(authentication, "system:permission:remove");
        adminMenuService.deleteMenu(id);
        return Result.success();
    }
}
