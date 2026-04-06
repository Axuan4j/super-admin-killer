package com.superkiller.backend.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String nickName;
    private String email;
    private String phone;
    private String avatar;
}
