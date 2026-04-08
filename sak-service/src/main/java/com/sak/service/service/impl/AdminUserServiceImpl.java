package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.RoleOptionResponse;
import com.sak.service.dto.UserAdminResponse;
import com.sak.service.dto.UserQueryRequest;
import com.sak.service.dto.UserSaveRequest;
import com.sak.service.entity.SysRole;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysRoleMapper;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.AdminUserService;
import com.sak.service.service.UserRoleRelationService;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final Map<String, String> USER_SORT_FIELDS = new LinkedHashMap<>();

    static {
        USER_SORT_FIELDS.put("id", "id");
        USER_SORT_FIELDS.put("username", "username");
        USER_SORT_FIELDS.put("nickName", "nick_name");
        USER_SORT_FIELDS.put("email", "email");
        USER_SORT_FIELDS.put("status", "status");
    }

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserRoleRelationService userRoleRelationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResponse<UserAdminResponse> listUsers(UserQueryRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getUsername, request.getKeyword())
                    .or()
                    .like(SysUser::getNickName, request.getKeyword())
                    .or()
                    .like(SysUser::getEmail, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(SysUser::getStatus, request.getStatus());
        }

        Page<SysUser> page = sysUserMapper.selectPage(PageUtils.buildPage(request, USER_SORT_FIELDS, "id", "asc"), queryWrapper);
        List<SysUser> users = page.getRecords();
        if (users.isEmpty()) {
            return PageUtils.toResponse(page, List.of(), request);
        }

        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        Map<Long, List<Long>> userRoleIds = userRoleRelationService.getRoleIdsByUserIds(userIds);

        List<Long> allRoleIds = userRoleIds.values().stream().flatMap(List::stream).distinct().toList();
        Map<Long, SysRole> roleMap = allRoleIds.isEmpty()
                ? Collections.emptyMap()
                : sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, allRoleIds))
                .stream()
                .collect(Collectors.toMap(SysRole::getId, role -> role));

        List<UserAdminResponse> records = users.stream()
                .map(user -> toResponse(user, userRoleIds.getOrDefault(user.getId(), List.of()), roleMap))
                .toList();
        return PageUtils.toResponse(page, records, request);
    }

    @Override
    @Transactional
    @LogRecord(success = "新增用户：{{#p0.username}}", fail = "新增用户失败：{{#p0.username}}", type = "USER", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public UserAdminResponse createUser(UserSaveRequest request) {
        validateUsernameUnique(request.getUsername(), null);
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("新增用户时密码不能为空");
        }

        SysUser user = new SysUser();
        applyRequest(user, request, true);
        sysUserMapper.insert(user);
        userRoleRelationService.replaceUserRoles(user.getId(), request.getRoleIds());
        return getUserById(user.getId());
    }

    @Override
    @Transactional
    @LogRecord(success = "编辑用户：{{#p1.username}}", fail = "编辑用户失败：{{#p1.username}}", type = "USER", subType = "UPDATE", bizNo = "{{#p0}}")
    public UserAdminResponse updateUser(Long id, UserSaveRequest request) {
        SysUser user = requireUser(id);
        validateUsernameUnique(request.getUsername(), id);
        applyRequest(user, request, false);
        sysUserMapper.updateById(user);
        userRoleRelationService.replaceUserRoles(id, request.getRoleIds());
        return getUserById(id);
    }

    @Override
    @Transactional
    @LogRecord(success = "删除用户：{{#p0}}", fail = "删除用户失败：{{#p0}}", type = "USER", subType = "DELETE", bizNo = "{{#p0}}")
    public void deleteUser(Long id) {
        userRoleRelationService.replaceUserRoles(id, List.of());
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
        List<Long> roleIds = userRoleRelationService.getRoleIdsByUserId(id);
        Map<Long, SysRole> roleMap = roleIds.isEmpty()
                ? Collections.emptyMap()
                : sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds))
                .stream()
                .collect(Collectors.toMap(SysRole::getId, role -> role));
        return toResponse(user, roleIds, roleMap);
    }

    private UserAdminResponse toResponse(SysUser user, List<Long> roleIds, Map<Long, SysRole> roleMap) {
        UserAdminResponse response = new UserAdminResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickName(user.getNickName());
        response.setEmail(user.getEmail());
        response.setWxPusherUid(user.getWxPusherUid());
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
        user.setWxPusherUid(request.getWxPusherUid());
        user.setPhone(request.getPhone());
        user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        user.setRemark(request.getRemark());
        if (isCreate || StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
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
