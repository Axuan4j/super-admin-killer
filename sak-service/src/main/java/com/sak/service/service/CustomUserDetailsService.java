package com.sak.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sak.service.entity.SysMenu;
import com.sak.service.entity.SysRole;
import com.sak.service.entity.SysUser;
import com.sak.service.entity.SysRoleMenu;
import com.sak.service.entity.SysUserRole;
import com.sak.service.mapper.SysMenuMapper;
import com.sak.service.mapper.SysRoleMapper;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.mapper.SysRoleMenuMapper;
import com.sak.service.mapper.SysUserRoleMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public CustomUserDetailsService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                                    SysUserRoleMapper userRoleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 查询用户角色
        Set<String> permissions = getPermissionsByUserId(user.getId());

        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new User(user.getUsername(), user.getPassword(), authorities);
    }

    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    public Set<String> getPermissionsByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        return getPermissionsByRoleIds(roleIds);
    }

    public Set<String> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toSet());

        if (menuIds.isEmpty()) {
            return Set.of();
        }

        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(menu -> "0".equals(menu.getVisible()))
                .filter(menu -> "F".equalsIgnoreCase(menu.getMenuType()) || "C".equalsIgnoreCase(menu.getMenuType()))
                .map(SysMenu::getPerms)
                .filter(perms -> perms != null && !perms.isBlank())
                .collect(Collectors.toSet());
    }

    public List<SysRole> getRolesByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        return roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> "0".equals(role.getStatus()))
                .collect(Collectors.toList());
    }
}
