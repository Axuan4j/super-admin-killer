package com.sak.service.service;

import com.sak.service.dto.PageResponse;
import com.sak.service.dto.MenuAdminResponse;
import com.sak.service.dto.MenuSaveRequest;

public interface AdminMenuService {
    PageResponse<MenuAdminResponse> listMenus(String keyword, String menuType, long current, long size);

    MenuAdminResponse createMenu(MenuSaveRequest request);

    MenuAdminResponse updateMenu(Long id, MenuSaveRequest request);

    void deleteMenu(Long id);
}
