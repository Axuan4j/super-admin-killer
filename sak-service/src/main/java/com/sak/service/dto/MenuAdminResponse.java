package com.sak.service.dto;

import lombok.Data;

@Data
public class MenuAdminResponse {
    private Long id;
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private String menuType;
    private String visible;
    private String perms;
    private String icon;
    private String remark;
}
