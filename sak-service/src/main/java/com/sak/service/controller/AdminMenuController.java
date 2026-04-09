package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.MenuAdminResponse;
import com.sak.service.convert.RequestVoConverter;
import com.sak.service.service.AdminMenuService;
import com.sak.service.vo.MenuSaveVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;
    private final RequestVoConverter requestVoMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<List<MenuAdminResponse>> listMenus(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "menuType", required = false) String menuType) {
        return Result.success(adminMenuService.listMenus(keyword, menuType));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:permission:add')")
    public Result<MenuAdminResponse> createMenu(@Valid @RequestBody MenuSaveVO request) {
        return Result.success(adminMenuService.createMenu(requestVoMapper.toMenuSaveRequest(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    public Result<MenuAdminResponse> updateMenu(@PathVariable("id") Long id, @Valid @RequestBody MenuSaveVO request) {
        return Result.success(adminMenuService.updateMenu(id, requestVoMapper.toMenuSaveRequest(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:remove')")
    public Result<Void> deleteMenu(@PathVariable("id") Long id) {
        adminMenuService.deleteMenu(id);
        return Result.success();
    }

    @PostMapping("/refresh-cache")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<Void> refreshPermissionCache() {
        adminMenuService.clearPermissionCaches();
        return Result.success();
    }
}
