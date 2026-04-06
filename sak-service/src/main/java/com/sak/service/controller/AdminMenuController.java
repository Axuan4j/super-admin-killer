package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.MenuAdminResponse;
import com.sak.service.dto.MenuSaveRequest;
import com.sak.service.service.AdminMenuService;
import com.sak.service.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<PageResponse<MenuAdminResponse>> listMenus(
            Authentication authentication,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "menuType", required = false) String menuType,
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "12") long size
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
