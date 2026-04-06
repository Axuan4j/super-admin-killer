package com.superkiller.backend.dto;

import lombok.Data;

@Data
public class RoleSaveRequest {
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String status;
    private String remark;
}
