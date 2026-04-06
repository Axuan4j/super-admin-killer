package com.superkiller.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.RoleOptionResponse;
import com.superkiller.backend.dto.UserAdminResponse;
import com.superkiller.backend.dto.UserSaveRequest;
import com.superkiller.backend.entity.SysRole;
import com.superkiller.backend.entity.SysUser;
import com.superkiller.backend.entity.SysUserRole;
import com.superkiller.backend.mapper.SysRoleMapper;
import com.superkiller.backend.mapper.SysUserMapper;
import com.superkiller.backend.mapper.SysUserRoleMapper;
import com.superkiller.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResponse<UserAdminResponse> listUsers(String keyword, String status, long current, long size) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .orderByAsc(SysUser::getId);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickName, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysUser::getStatus, status);
        }

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(current, size), queryWrapper);
        List<SysUser> users = page.getRecords();
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(null);
        Map<Long, List<Long>> userRoleIds = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));

        List<Long> allRoleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, SysRole> roleMap = allRoleIds.isEmpty()
                ? Collections.emptyMap()
                : sysRoleMapper.selectBatchIds(allRoleIds).stream().collect(Collectors.toMap(SysRole::getId, role -> role));

        List<UserAdminResponse> records = users.stream()
                .map(user -> toResponse(user, userRoleIds.getOrDefault(user.getId(), List.of()), roleMap))
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    @LogRecord(success = "新增用户：{{#request.username}}", fail = "新增用户失败：{{#request.username}}", type = "USER", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public UserAdminResponse createUser(UserSaveRequest request) {
        validateUsernameUnique(request.getUsername(), null);
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("新增用户时密码不能为空");
        }

        SysUser user = new SysUser();
        applyRequest(user, request, true);
        sysUserMapper.insert(user);
        replaceUserRoles(user.getId(), request.getRoleIds());
        return getUserById(user.getId());
    }

    @Override
    @Transactional
    @LogRecord(success = "编辑用户：{{#request.username}}", fail = "编辑用户失败：{{#request.username}}", type = "USER", subType = "UPDATE", bizNo = "{{#id}}")
    public UserAdminResponse updateUser(Long id, UserSaveRequest request) {
        SysUser user = requireUser(id);
        validateUsernameUnique(request.getUsername(), id);
        applyRequest(user, request, false);
        sysUserMapper.updateById(user);
        replaceUserRoles(id, request.getRoleIds());
        return getUserById(id);
    }

    @Override
    @Transactional
    @LogRecord(success = "删除用户：{{#id}}", fail = "删除用户失败：{{#id}}", type = "USER", subType = "DELETE", bizNo = "{{#id}}")
    public void deleteUser(Long id) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        sysUserMapper.deleteById(id);
    }

    @Override
    public List<RoleOptionResponse> listRoleOptions() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getRoleSort).orderByAsc(SysRole::getId))
                .stream()
                .map(role -> new RoleOptionResponse(role.getId(), role.getRoleName(), role.getRoleKey(), role.getRoleSort(), role.getStatus(), role.getRemark()))
                .toList();
    }

    private UserAdminResponse getUserById(Long id) {
        SysUser user = requireUser(id);
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        Map<Long, SysRole> roleMap = roleIds.isEmpty()
                ? Collections.emptyMap()
                : sysRoleMapper.selectBatchIds(roleIds).stream().collect(Collectors.toMap(SysRole::getId, role -> role));
        return toResponse(user, roleIds, roleMap);
    }

    private UserAdminResponse toResponse(SysUser user, List<Long> roleIds, Map<Long, SysRole> roleMap) {
        UserAdminResponse response = new UserAdminResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickName(user.getNickName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setRemark(user.getRemark());
        response.setRoleIds(roleIds);
        response.setRoleNames(roleIds.stream()
                .map(roleMap::get)
                .filter(Objects::nonNull)
                .map(SysRole::getRoleName)
                .toList());
        return response;
    }

    private void applyRequest(SysUser user, UserSaveRequest request, boolean isCreate) {
        user.setUsername(request.getUsername());
        user.setNickName(request.getNickName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        user.setRemark(request.getRemark());
        if (isCreate || StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
    }

    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.stream().distinct().forEach(roleId -> {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        });
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private void validateUsernameUnique(String username, Long excludeId) {
        SysUser exists = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("limit 1"));
        if (exists != null && !Objects.equals(exists.getId(), excludeId)) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }
}
