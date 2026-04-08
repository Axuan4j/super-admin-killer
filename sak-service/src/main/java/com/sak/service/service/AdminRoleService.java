package com.sak.service.service;

import com.sak.service.dto.PageResponse;
import com.sak.service.dto.RoleOptionResponse;
import com.sak.service.dto.RoleQueryRequest;
import com.sak.service.dto.RoleSaveRequest;

import java.util.List;

public interface AdminRoleService {
    PageResponse<RoleOptionResponse> listRoles(RoleQueryRequest request);

    RoleOptionResponse createRole(RoleSaveRequest request);

    RoleOptionResponse updateRole(Long id, RoleSaveRequest request);

    void deleteRole(Long id);

    List<Long> getRoleMenuIds(Long id);

    void updateRoleMenus(Long id, List<Long> menuIds);
}
