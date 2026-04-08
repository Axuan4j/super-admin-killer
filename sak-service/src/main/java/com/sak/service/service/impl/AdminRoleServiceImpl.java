package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.RoleQueryRequest;
import com.sak.service.dto.RoleOptionResponse;
import com.sak.service.dto.RoleSaveRequest;
import com.sak.service.entity.SysRole;
import com.sak.service.mapper.SysRoleMapper;
import com.sak.service.service.AdminRoleService;
import com.sak.service.service.RoleMenuRelationService;
import com.sak.service.service.UserRoleRelationService;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private static final String ROLE_MENU_IDS_CACHE = "role-menu-ids";
    private static final String CURRENT_USER_MENUS_CACHE = "current-user-menus";
    private static final Map<String, String> ROLE_SORT_FIELDS = new LinkedHashMap<>();

    static {
        ROLE_SORT_FIELDS.put("id", "id");
        ROLE_SORT_FIELDS.put("roleName", "role_name");
        ROLE_SORT_FIELDS.put("roleKey", "role_key");
        ROLE_SORT_FIELDS.put("roleSort", "role_sort");
        ROLE_SORT_FIELDS.put("status", "status");
    }

    private final SysRoleMapper sysRoleMapper;
    private final UserRoleRelationService userRoleRelationService;
    private final RoleMenuRelationService roleMenuRelationService;
    private final CacheManager cacheManager;

    @Override
    public PageResponse<RoleOptionResponse> listRoles(RoleQueryRequest request) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysRole::getRoleName, request.getKeyword())
                    .or()
                    .like(SysRole::getRoleKey, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(SysRole::getStatus, request.getStatus());
        }

        Page<SysRole> page = sysRoleMapper.selectPage(PageUtils.buildPage(request, ROLE_SORT_FIELDS, "role_sort", "asc"), queryWrapper);
        List<RoleOptionResponse> records = page.getRecords().stream()
                .map(role -> new RoleOptionResponse(role.getId(), role.getRoleName(), role.getRoleKey(), role.getRoleSort(), role.getStatus(), role.getRemark()))
                .toList();
        return PageUtils.toResponse(page, records, request);
    }

    @Override
    @LogRecord(success = "新增角色：{{#p0.roleName}}", fail = "新增角色失败：{{#p0.roleName}}", type = "ROLE", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public RoleOptionResponse createRole(RoleSaveRequest request) {
        validateRoleKeyUnique(request.getRoleKey(), null);
        SysRole role = new SysRole();
        applyRequest(role, request);
        sysRoleMapper.insert(role);
        return new RoleOptionResponse(role.getId(), role.getRoleName(), role.getRoleKey(), role.getRoleSort(), role.getStatus(), role.getRemark());
    }

    @Override
    @LogRecord(success = "编辑角色：{{#p1.roleName}}", fail = "编辑角色失败：{{#p1.roleName}}", type = "ROLE", subType = "UPDATE", bizNo = "{{#p0}}")
    public RoleOptionResponse updateRole(Long id, RoleSaveRequest request) {
        SysRole role = requireRole(id);
        validateRoleKeyUnique(request.getRoleKey(), id);
        applyRequest(role, request);
        sysRoleMapper.updateById(role);
        return new RoleOptionResponse(role.getId(), role.getRoleName(), role.getRoleKey(), role.getRoleSort(), role.getStatus(), role.getRemark());
    }

    @Override
    @Transactional
    @LogRecord(success = "删除角色：{{#p0}}", fail = "删除角色失败：{{#p0}}", type = "ROLE", subType = "DELETE", bizNo = "{{#p0}}")
    public void deleteRole(Long id) {
        long bindCount = userRoleRelationService.countUsersByRoleId(id);
        if (bindCount > 0) {
            throw new IllegalArgumentException("该角色已绑定用户，无法删除");
        }
        roleMenuRelationService.replaceRoleMenus(id, List.of());
        sysRoleMapper.deleteById(id);
        evictRoleMenuCache(id);
        clearCurrentUserMenusCache();
    }

    @Override
    @Cacheable(cacheNames = ROLE_MENU_IDS_CACHE, key = "#p0")
    public List<Long> getRoleMenuIds(Long id) {
        requireRole(id);
        return roleMenuRelationService.getMenuIdsByRoleId(id);
    }

    @Override
    @Transactional
    @LogRecord(success = "更新角色权限：{{#p0}}", fail = "更新角色权限失败：{{#p0}}", type = "ROLE", subType = "GRANT", bizNo = "{{#p0}}")
    public void updateRoleMenus(Long id, List<Long> menuIds) {
        requireRole(id);
        List<Long> distinctMenuIds = normalizeMenuIds(menuIds);

        if (distinctMenuIds.isEmpty()) {
            roleMenuRelationService.replaceRoleMenus(id, List.of());
            putRoleMenuCache(id, distinctMenuIds);
            clearCurrentUserMenusCache();
            return;
        }
        roleMenuRelationService.replaceRoleMenus(id, distinctMenuIds);
        putRoleMenuCache(id, distinctMenuIds);
        clearCurrentUserMenusCache();
    }

    private void applyRequest(SysRole role, RoleSaveRequest request) {
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setRoleSort(request.getRoleSort() == null ? 0 : request.getRoleSort());
        role.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        role.setRemark(request.getRemark());
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return role;
    }

    private void validateRoleKeyUnique(String roleKey, Long excludeId) {
        SysRole exists = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey).last("limit 1"));
        if (exists != null && !Objects.equals(exists.getId(), excludeId)) {
            throw new IllegalArgumentException("角色标识已存在");
        }
    }

    private List<Long> normalizeMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return menuIds.stream().distinct().toList();
    }

    private void putRoleMenuCache(Long roleId, List<Long> menuIds) {
        Cache cache = cacheManager.getCache(ROLE_MENU_IDS_CACHE);
        if (cache != null) {
            cache.put(roleId, menuIds);
        }
    }

    private void evictRoleMenuCache(Long roleId) {
        Cache cache = cacheManager.getCache(ROLE_MENU_IDS_CACHE);
        if (cache != null) {
            cache.evict(roleId);
        }
    }

    private void clearCurrentUserMenusCache() {
        Cache cache = cacheManager.getCache(CURRENT_USER_MENUS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }
}
