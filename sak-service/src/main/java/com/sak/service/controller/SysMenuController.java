package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.MenuResponse;
import com.sak.service.service.MenuService;
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
