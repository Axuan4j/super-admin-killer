package com.sak.service.service;

import com.sak.service.dto.LoginResponse;
import com.sak.service.dto.MfaCodeRequest;
import com.sak.service.dto.MfaLoginVerifyRequest;
import com.sak.service.dto.MfaSetupResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

public interface MfaService {

    LoginResponse handleLoginSuccess(UserDetails userDetails, HttpServletRequest request);

    LoginResponse verifyLoginCode(MfaLoginVerifyRequest request, HttpServletRequest servletRequest);

    MfaSetupResponse createSetup(String username);

    void enableMfa(String username, MfaCodeRequest request);

    void disableMfa(String username, MfaCodeRequest request);
}
