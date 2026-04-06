package com.superkiller.backend.service;

import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.RoleOptionResponse;
import com.superkiller.backend.dto.UserAdminResponse;
import com.superkiller.backend.dto.UserSaveRequest;

import java.util.List;

public interface AdminUserService {
    PageResponse<UserAdminResponse> listUsers(String keyword, String status, long current, long size);

    UserAdminResponse createUser(UserSaveRequest request);

    UserAdminResponse updateUser(Long id, UserSaveRequest request);

    void deleteUser(Long id);

    List<RoleOptionResponse> listRoleOptions();
}
