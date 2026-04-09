package com.sak.service.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshVO {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
