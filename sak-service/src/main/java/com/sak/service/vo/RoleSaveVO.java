package com.sak.service.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleSaveVO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64位")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Size(max = 64, message = "角色标识长度不能超过64位")
    private String roleKey;

    private Integer roleSort;

    @Pattern(regexp = "0|1", message = "状态值不合法")
    private String status;

    @Size(max = 255, message = "备注长度不能超过255位")
    private String remark;
}
