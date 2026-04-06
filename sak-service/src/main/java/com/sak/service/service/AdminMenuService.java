package com.sak.service.service;

import com.sak.service.dto.PageResponse;
import com.sak.service.dto.MenuAdminResponse;
import com.sak.service.dto.MenuSaveRequest;

import java.util.List;

public interface AdminMenuService {

    List<MenuAdminResponse> listMenus(String keyword, String menuType);

    MenuAdminResponse createMenu(MenuSaveRequest request);

    MenuAdminResponse updateMenu(Long id, MenuSaveRequest request);

    void deleteMenu(Long id);
}
