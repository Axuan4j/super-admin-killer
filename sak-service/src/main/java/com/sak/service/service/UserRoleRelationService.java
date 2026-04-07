package com.sak.service.service;

import java.util.List;
import java.util.Map;

public interface UserRoleRelationService {
    List<Long> getRoleIdsByUserId(Long userId);

    Map<Long, List<Long>> getRoleIdsByUserIds(List<Long> userIds);

    long countUsersByRoleId(Long roleId);

    void replaceUserRoles(Long userId, List<Long> roleIds);
}
