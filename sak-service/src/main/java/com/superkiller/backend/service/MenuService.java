package com.superkiller.backend.service;

import com.superkiller.backend.dto.MenuResponse;

import java.util.List;

public interface MenuService {
    List<MenuResponse> getCurrentUserMenus(String username);
}
