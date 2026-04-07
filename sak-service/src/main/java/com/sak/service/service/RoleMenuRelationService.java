package com.sak.service.service;

import java.util.List;
import java.util.Set;

public interface RoleMenuRelationService {
    List<Long> getMenuIdsByRoleId(Long roleId);

    Set<Long> getMenuIdsByRoleIds(List<Long> roleIds);

    void replaceRoleMenus(Long roleId, List<Long> menuIds);
}
