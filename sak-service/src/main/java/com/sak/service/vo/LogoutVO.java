package com.sak.service.vo;

import lombok.Data;

@Data
public class LogoutVO {

    private String accessToken;

    private String refreshToken;

    private String token;
}
