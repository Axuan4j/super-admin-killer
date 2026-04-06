package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.dto.MenuResponse;
import com.superkiller.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final MenuService menuService;

    @GetMapping("/current")
    public Result<List<MenuResponse>> getCurrentUserMenus(Authentication authentication) {
        return Result.success(menuService.getCurrentUserMenus(authentication.getName()));
    }
}
