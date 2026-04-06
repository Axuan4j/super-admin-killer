package com.superkiller.backend.service;

import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.RoleOptionResponse;
import com.superkiller.backend.dto.RoleSaveRequest;

import java.util.List;

public interface AdminRoleService {
    PageResponse<RoleOptionResponse> listRoles(String keyword, String status, long current, long size);

    RoleOptionResponse createRole(RoleSaveRequest request);

    RoleOptionResponse updateRole(Long id, RoleSaveRequest request);

    void deleteRole(Long id);

    List<Long> getRoleMenuIds(Long id);

    void updateRoleMenus(Long id, List<Long> menuIds);
}
