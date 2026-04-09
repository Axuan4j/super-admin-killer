package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfaSetupResponse {
    private String issuer;
    private String accountName;
    private String secret;
    private String otpauthUri;
    private String qrCodeBase64;
}
