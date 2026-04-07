package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sak.service.entity.SysUserRole;
import com.sak.service.mapper.SysUserRoleMapper;
import com.sak.service.service.UserRoleRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleRelationServiceImpl implements UserRoleRelationService {

    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
    }

    @Override
    public Map<Long, List<Long>> getRoleIdsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(
                        SysUserRole::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(SysUserRole::getRoleId,
                                Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf))
                ));
    }

    @Override
    public long countUsersByRoleId(Long roleId) {
        if (roleId == null) {
            return 0L;
        }
        return sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
    }

    @Override
    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<Long> distinctRoleIds = new LinkedHashSet<>(roleIds);
        distinctRoleIds.forEach(roleId -> {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        });
    }
}
