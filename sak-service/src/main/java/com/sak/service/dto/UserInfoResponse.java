package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String nickName;
    private String email;
    private String wxPusherUid;
    private String phone;
    private String avatar;
    private List<String> roles;
    private List<String> authorities;
}
