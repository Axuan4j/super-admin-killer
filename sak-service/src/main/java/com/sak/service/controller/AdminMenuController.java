package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.MenuAdminResponse;
import com.sak.service.dto.MenuSaveRequest;
import com.sak.service.service.AdminMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<List<MenuAdminResponse>> listMenus(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "menuType", required = false) String menuType) {
        return Result.success(adminMenuService.listMenus(keyword, menuType));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:permission:add')")
    public Result<MenuAdminResponse> createMenu(@RequestBody MenuSaveRequest request) {
        return Result.success(adminMenuService.createMenu(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    public Result<MenuAdminResponse> updateMenu(@PathVariable("id") Long id, @RequestBody MenuSaveRequest request) {
        return Result.success(adminMenuService.updateMenu(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:remove')")
    public Result<Void> deleteMenu(@PathVariable("id") Long id) {
        adminMenuService.deleteMenu(id);
        return Result.success();
    }
}
