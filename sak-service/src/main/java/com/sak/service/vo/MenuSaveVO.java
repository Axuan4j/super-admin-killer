package com.sak.service.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuSaveVO {

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称长度不能超过64位")
    private String menuName;

    private Long parentId;

    private Integer orderNum;

    @Size(max = 255, message = "路由路径长度不能超过255位")
    private String path;

    @Size(max = 255, message = "组件路径长度不能超过255位")
    private String component;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "M|C|F", message = "菜单类型不合法")
    private String menuType;

    @Pattern(regexp = "0|1", message = "显示状态不合法")
    private String visible;

    @Size(max = 128, message = "权限标识长度不能超过128位")
    private String perms;

    @Size(max = 128, message = "图标长度不能超过128位")
    private String icon;

    @Size(max = 255, message = "备注长度不能超过255位")
    private String remark;
}
