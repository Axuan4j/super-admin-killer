package com.sak.service.dto;

import lombok.Data;

@Data
public class MfaLoginVerifyRequest {
    private String challengeToken;
    private String code;
}
