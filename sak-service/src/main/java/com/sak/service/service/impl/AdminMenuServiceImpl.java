package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.MenuAdminResponse;
import com.sak.service.dto.MenuSaveRequest;
import com.sak.service.entity.SysMenu;
import com.sak.service.entity.SysRoleMenu;
import com.sak.service.mapper.SysMenuMapper;
import com.sak.service.mapper.SysRoleMenuMapper;
import com.sak.service.service.AdminMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMenuServiceImpl implements AdminMenuService {

    private static final String ROLE_MENU_IDS_CACHE = "role-menu-ids";
    private static final String ADMIN_MANAGE_MENUS_CACHE = "admin-manage-menus";
    private static final String CURRENT_USER_MENUS_CACHE = "current-user-menus";

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final CacheManager cacheManager;

    @Override
    public List<MenuAdminResponse> listMenus(String keyword, String menuType) {
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum)
                .orderByAsc(SysMenu::getId);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysMenu::getMenuName, keyword)
                    .or()
                    .like(SysMenu::getPath, keyword)
                    .or()
                    .like(SysMenu::getPerms, keyword));
        }
        if (StringUtils.hasText(menuType)) {
            queryWrapper.eq(SysMenu::getMenuType, menuType);
        }

        List<SysMenu> sysMenus = sysMenuMapper.selectList(queryWrapper);
        return sysMenus.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @LogRecord(success = "新增菜单：{{#p0.menuName}}", fail = "新增菜单失败：{{#p0.menuName}}", type = "MENU", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public MenuAdminResponse createMenu(MenuSaveRequest request) {
        SysMenu menu = new SysMenu();
        applyRequest(menu, request);
        sysMenuMapper.insert(menu);
        clearMenuRelatedCaches();
        return toResponse(menu);
    }

    @Override
    @LogRecord(success = "编辑菜单：{{#p1.menuName}}", fail = "编辑菜单失败：{{#p1.menuName}}", type = "MENU", subType = "UPDATE", bizNo = "{{#p0}}")
    public MenuAdminResponse updateMenu(Long id, MenuSaveRequest request) {
        SysMenu menu = requireMenu(id);
        applyRequest(menu, request);
        sysMenuMapper.updateById(menu);
        clearMenuRelatedCaches();
        return toResponse(menu);
    }

    @Override
    @Transactional
    @LogRecord(success = "删除菜单：{{#p0}}", fail = "删除菜单失败：{{#p0}}", type = "MENU", subType = "DELETE", bizNo = "{{#p0}}")
    public void deleteMenu(Long id) {
        long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            throw new IllegalArgumentException("请先删除子菜单或按钮");
        }
        List<Long> affectedRoleIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id))
                .stream()
                .map(SysRoleMenu::getRoleId)
                .distinct()
                .toList();
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        sysMenuMapper.deleteById(id);
        evictRoleMenuCache(affectedRoleIds);
        clearMenuRelatedCaches();
    }

    private void applyRequest(SysMenu menu, MenuSaveRequest request) {
        menu.setMenuName(request.getMenuName());
        menu.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        menu.setOrderNum(request.getOrderNum() == null ? 0 : request.getOrderNum());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setMenuType(request.getMenuType());
        menu.setVisible(StringUtils.hasText(request.getVisible()) ? request.getVisible() : "0");
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setRemark(request.getRemark());
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new IllegalArgumentException("菜单不存在");
        }
        return menu;
    }

    private MenuAdminResponse toResponse(SysMenu menu) {
        MenuAdminResponse response = new MenuAdminResponse();
        response.setId(menu.getId());
        response.setMenuName(menu.getMenuName());
        response.setParentId(menu.getParentId());
        response.setOrderNum(menu.getOrderNum());
        response.setPath(menu.getPath());
        response.setComponent(menu.getComponent());
        response.setMenuType(menu.getMenuType());
        response.setVisible(menu.getVisible());
        response.setPerms(menu.getPerms());
        response.setIcon(menu.getIcon());
        response.setRemark(menu.getRemark());
        return response;
    }

    private void evictRoleMenuCache(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Cache cache = cacheManager.getCache(ROLE_MENU_IDS_CACHE);
        if (cache == null) {
            return;
        }
        roleIds.forEach(cache::evict);
    }

    private void clearMenuRelatedCaches() {
        clearCache(ADMIN_MANAGE_MENUS_CACHE);
        clearCache(CURRENT_USER_MENUS_CACHE);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
