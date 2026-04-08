package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.RoleOptionResponse;
import com.sak.service.dto.UserAdminResponse;
import com.sak.service.dto.UserQueryRequest;
import com.sak.service.dto.UserSaveRequest;
import com.sak.service.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<PageResponse<UserAdminResponse>> listUsers(UserQueryRequest request) {
        return Result.success(adminUserService.listUsers(request));
    }

    @GetMapping("/role-options")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<RoleOptionResponse>> listRoleOptions() {
        return Result.success(adminUserService.listRoleOptions());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<UserAdminResponse> createUser(@RequestBody UserSaveRequest request) {
        return Result.success(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<UserAdminResponse> updateUser(@PathVariable("id") Long id, @RequestBody UserSaveRequest request) {
        return Result.success(adminUserService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:remove')")
    public Result<Void> deleteUser(@PathVariable("id") Long id) {
        adminUserService.deleteUser(id);
        return Result.success();
    }
}
