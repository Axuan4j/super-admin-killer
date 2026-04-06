package com.sak.service.service;

import com.sak.service.dto.MenuResponse;

import java.util.List;

public interface MenuService {
    List<MenuResponse> getCurrentUserMenus(String username);
}
