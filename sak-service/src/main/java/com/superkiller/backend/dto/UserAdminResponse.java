package com.superkiller.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserAdminResponse {
    private Long id;
    private String username;
    private String nickName;
    private String email;
    private String phone;
    private String status;
    private String remark;
    private List<Long> roleIds;
    private List<String> roleNames;
}
