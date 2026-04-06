package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {
    private Long id;
    private String path;
    private String name;
    private String component;
    private String icon;
    private String layout;
    private String perms;
    private List<MenuResponse> children = new ArrayList<>();
}
