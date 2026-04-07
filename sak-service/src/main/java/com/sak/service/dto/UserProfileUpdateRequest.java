package com.sak.service.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String nickName;
    private String email;
    private String wxPusherUid;
    private String phone;
    private String avatar;
}
