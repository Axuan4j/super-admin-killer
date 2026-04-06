package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sak.service.dto.MenuResponse;
import com.sak.service.entity.SysMenu;
import com.sak.service.entity.SysRoleMenu;
import com.sak.service.entity.SysUser;
import com.sak.service.entity.SysUserRole;
import com.sak.service.mapper.SysMenuMapper;
import com.sak.service.mapper.SysRoleMenuMapper;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.mapper.SysUserRoleMapper;
import com.sak.service.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<MenuResponse> getCurrentUserMenus(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            return Collections.emptyList();
        }

        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toSet());
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds).stream()
                .filter(menu -> "0".equals(menu.getVisible()))
                .filter(menu -> !"F".equalsIgnoreCase(menu.getMenuType()))
                .sorted(Comparator.comparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysMenu::getId))
                .toList();

        Map<Long, MenuResponse> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            menuMap.put(menu.getId(), toMenuResponse(menu));
        }

        List<MenuResponse> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuResponse current = menuMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0 || !menuMap.containsKey(parentId)) {
                roots.add(current);
                continue;
            }
            menuMap.get(parentId).getChildren().add(current);
        }

        return roots;
    }

    private MenuResponse toMenuResponse(SysMenu menu) {
        String layout = menu.getPath() != null && menu.getPath().startsWith("/blank") ? "blank" : "default";
        return new MenuResponse(
                menu.getId(),
                menu.getPath(),
                menu.getMenuName(),
                menu.getComponent(),
                menu.getIcon(),
                layout,
                menu.getPerms(),
                new ArrayList<>()
        );
    }
}
