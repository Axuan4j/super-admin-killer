package com.sak.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserSaveRequest {
    private String username;
    private String password;
    private String nickName;
    private String email;
    private String wxPusherUid;
    private String phone;
    private String status;
    private String remark;
    private List<Long> roleIds;
}
