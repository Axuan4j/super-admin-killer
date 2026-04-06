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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMenuServiceImpl implements AdminMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public PageResponse<MenuAdminResponse> listMenus(String keyword, String menuType, long current, long size) {
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

        Page<SysMenu> page = sysMenuMapper.selectPage(new Page<>(current, size), queryWrapper);
        List<MenuAdminResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @LogRecord(success = "新增菜单：{{#request.menuName}}", fail = "新增菜单失败：{{#request.menuName}}", type = "MENU", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public MenuAdminResponse createMenu(MenuSaveRequest request) {
        SysMenu menu = new SysMenu();
        applyRequest(menu, request);
        sysMenuMapper.insert(menu);
        return toResponse(menu);
    }

    @Override
    @LogRecord(success = "编辑菜单：{{#request.menuName}}", fail = "编辑菜单失败：{{#request.menuName}}", type = "MENU", subType = "UPDATE", bizNo = "{{#id}}")
    public MenuAdminResponse updateMenu(Long id, MenuSaveRequest request) {
        SysMenu menu = requireMenu(id);
        applyRequest(menu, request);
        sysMenuMapper.updateById(menu);
        return toResponse(menu);
    }

    @Override
    @Transactional
    @LogRecord(success = "删除菜单：{{#id}}", fail = "删除菜单失败：{{#id}}", type = "MENU", subType = "DELETE", bizNo = "{{#id}}")
    public void deleteMenu(Long id) {
        long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            throw new IllegalArgumentException("请先删除子菜单或按钮");
        }
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        sysMenuMapper.deleteById(id);
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
}
