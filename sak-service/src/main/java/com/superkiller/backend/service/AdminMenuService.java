package com.superkiller.backend.service;

import com.superkiller.backend.dto.PageResponse;
import com.superkiller.backend.dto.MenuAdminResponse;
import com.superkiller.backend.dto.MenuSaveRequest;

import java.util.List;

public interface AdminMenuService {
    PageResponse<MenuAdminResponse> listMenus(String keyword, String menuType, long current, long size);

    MenuAdminResponse createMenu(MenuSaveRequest request);

    MenuAdminResponse updateMenu(Long id, MenuSaveRequest request);

    void deleteMenu(Long id);
}
