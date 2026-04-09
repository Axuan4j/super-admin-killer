package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private Boolean mfaRequired;
    private String challengeToken;

    public static LoginResponse loginSuccess(String accessToken, String refreshToken, String username) {
        return new LoginResponse(accessToken, refreshToken, username, false, null);
    }

    public static LoginResponse mfaChallenge(String username, String challengeToken) {
        return new LoginResponse(null, null, username, true, challengeToken);
    }
}
