package com.sak.service.service;

import com.sak.service.dto.PageResponse;
import com.sak.service.dto.RoleOptionResponse;
import com.sak.service.dto.UserAdminResponse;
import com.sak.service.dto.UserQueryRequest;
import com.sak.service.dto.UserSaveRequest;

import java.util.List;

public interface AdminUserService {
    PageResponse<UserAdminResponse> listUsers(UserQueryRequest request);

    UserAdminResponse createUser(UserSaveRequest request);

    UserAdminResponse updateUser(Long id, UserSaveRequest request);

    void deleteUser(Long id);

    List<RoleOptionResponse> listRoleOptions();
}
