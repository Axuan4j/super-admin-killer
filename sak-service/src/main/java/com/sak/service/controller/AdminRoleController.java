package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.RoleOptionResponse;
import com.sak.service.dto.RoleSaveRequest;
import com.sak.service.service.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<PageResponse<RoleOptionResponse>> listRoles(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size
    ) {
        return Result.success(adminRoleService.listRoles(keyword, status, current, size));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<RoleOptionResponse> createRole(@RequestBody RoleSaveRequest request) {
        return Result.success(adminRoleService.createRole(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<RoleOptionResponse> updateRole(@PathVariable("id") Long id, @RequestBody RoleSaveRequest request) {
        return Result.success(adminRoleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:remove')")
    public Result<Void> deleteRole(@PathVariable("id") Long id) {
        adminRoleService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/{id}/menu-ids")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<List<Long>> getRoleMenuIds(@PathVariable("id") Long id) {
        return Result.success(adminRoleService.getRoleMenuIds(id));
    }

    @PutMapping("/{id}/menu-ids")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> updateRoleMenuIds(@PathVariable("id") Long id, @RequestBody List<Long> menuIds) {
        adminRoleService.updateRoleMenus(id, menuIds);
        return Result.success();
    }
}
