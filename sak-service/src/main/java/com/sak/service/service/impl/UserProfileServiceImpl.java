package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.UserPasswordUpdateRequest;
import com.sak.service.dto.UserProfileUpdateRequest;
import com.sak.service.dto.UserInfoResponse;
import com.sak.service.entity.SysRole;
import com.sak.service.entity.SysUser;
import com.sak.service.entity.SysUserRole;
import com.sak.service.mapper.SysRoleMapper;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.mapper.SysUserRoleMapper;
import com.sak.service.service.CustomUserDetailsService;
import com.sak.service.service.UserProfileService;
import com.sak.service.util.ValidUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserInfoResponse getUserInfo(String username) {
        SysUser user = getRequiredUser(username);
        if (user == null) {
            return null;
        }

        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();

        List<String> roles = roleIds.isEmpty()
                ? Collections.emptyList()
                : sysRoleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleKey)
                .filter(roleKey -> roleKey != null && !roleKey.isBlank())
                .distinct()
                .collect(Collectors.toList());

        Set<String> permissions = customUserDetailsService.getPermissionsByUserId(user.getId());

        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                (user.getNickName() == null || user.getNickName().isBlank()) ? user.getUsername() : user.getNickName(),
                user.getEmail(),
                user.getWxPusherUid(),
                user.getPhone(),
                user.getAvatar(),
                Integer.valueOf(1).equals(user.getMfaEnabled()),
                roles,
                permissions.stream().sorted().collect(Collectors.toList())
        );
    }

    @Override
    @Transactional
    @LogRecord(success = "更新个人资料", fail = "更新个人资料失败", type = "PROFILE", subType = "UPDATE", bizNo = "{{#p0}}")
    public UserInfoResponse updateUserProfile(String username, UserProfileUpdateRequest request) {
        SysUser user = getRequiredUser(username);
        ValidUtil.check()
                .hasText(request.getNickName(), "昵称不能为空");
        user.setNickName(request.getNickName().trim());
        user.setEmail(normalize(request.getEmail()));
        user.setWxPusherUid(normalize(request.getWxPusherUid()));
        user.setPhone(normalize(request.getPhone()));
        user.setAvatar(normalize(request.getAvatar()));
        sysUserMapper.updateById(user);
        return getUserInfo(username);
    }

    @Override
    @Transactional
    @LogRecord(success = "修改个人密码", fail = "修改个人密码失败", type = "PROFILE", subType = "PASSWORD", bizNo = "{{#p0}}")
    public void updatePassword(String username, UserPasswordUpdateRequest request) {
        SysUser user = getRequiredUser(username);
        ValidUtil.check()
                .hasText(request.getOldPassword(), "原密码不能为空")
                .hasText(request.getNewPassword(), "新密码不能为空")
                .custom(() -> passwordEncoder.matches(request.getOldPassword(), user.getPassword()), "原密码不正确")
                .minLength(request.getNewPassword(), 6, "新密码至少 6 位")
                .custom(() -> !passwordEncoder.matches(request.getNewPassword().trim(), user.getPassword()), "新密码不能与原密码相同");
        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        sysUserMapper.updateById(user);
    }

    private SysUser getRequiredUser(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
