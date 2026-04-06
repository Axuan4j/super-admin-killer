package com.superkiller.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.RoleOptionResponse;
import com.superkiller.backend.dto.RoleSaveRequest;
import com.superkiller.backend.entity.SysRole;
import com.superkiller.backend.entity.SysRoleMenu;
import com.superkiller.backend.entity.SysUserRole;
import com.superkiller.backend.mapper.SysRoleMapper;
import com.superkiller.backend.mapper.SysRoleMenuMapper;
import com.superkiller.backend.mapper.SysUserRoleMapper;
import com.superkiller.backend.service.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public PageResponse<RoleOptionResponse> listRoles(String keyword, String status, long current, long size) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getRoleSort)
                .orderByAsc(SysRole::getId);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysRole::getRoleName, keyword)
                    .or()
                    .like(SysRole::getRoleKey, keyword));
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysRole::getStatus, status);
        }

        Page<SysRole> page = sysRoleMapper.selectPage(new Page<>(current, size), queryWrapper);
        List<RoleOptionResponse> records = page.getRecords().stream()
                .map(role -> new RoleOptionResponse(role.getId(), role.getRoleName(), role.getRoleKey(), role.getRoleSort(), role.getStatus(), role.getRemark()))
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @LogRecord(success = "新增角色：{{#request.roleName}}", fail = "新增角色失败：{{#request.roleName}}", type = "ROLE", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public RoleOptionResponse createRole(RoleSaveRequest request) {
        validateRoleKeyUnique(request.getRoleKey(), null);
        SysRole role = new SysRole();
        applyRequest(role, request);
        sysRoleMapper.insert(role);
        return new RoleOptionResponse(role.getId(), role.getRoleName(), role.getRoleKey(), role.getRoleSort(), role.getStatus(), role.getRemark());
    }

    @Override
    @LogRecord(success = "编辑角色：{{#request.roleName}}", fail = "编辑角色失败：{{#request.roleName}}", type = "ROLE", subType = "UPDATE", bizNo = "{{#id}}")
    public RoleOptionResponse updateRole(Long id, RoleSaveRequest request) {
        SysRole role = requireRole(id);
        validateRoleKeyUnique(request.getRoleKey(), id);
        applyRequest(role, request);
        sysRoleMapper.updateById(role);
        return new RoleOptionResponse(role.getId(), role.getRoleName(), role.getRoleKey(), role.getRoleSort(), role.getStatus(), role.getRemark());
    }

    @Override
    @Transactional
    @LogRecord(success = "删除角色：{{#id}}", fail = "删除角色失败：{{#id}}", type = "ROLE", subType = "DELETE", bizNo = "{{#id}}")
    public void deleteRole(Long id) {
        long bindCount = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (bindCount > 0) {
            throw new IllegalArgumentException("该角色已绑定用户，无法删除");
        }
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        sysRoleMapper.deleteById(id);
    }

    @Override
    public List<Long> getRoleMenuIds(Long id) {
        requireRole(id);
        return sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    @Override
    @Transactional
    @LogRecord(success = "更新角色权限：{{#id}}", fail = "更新角色权限失败：{{#id}}", type = "ROLE", subType = "GRANT", bizNo = "{{#id}}")
    public void updateRoleMenus(Long id, List<Long> menuIds) {
        requireRole(id);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        menuIds.stream().distinct().forEach(menuId -> {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(id);
            relation.setMenuId(menuId);
            sysRoleMenuMapper.insert(relation);
        });
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
}
