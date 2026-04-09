package com.sak.service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.common.Result;
import com.sak.service.dto.LoginResponse;
import com.sak.service.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
@RequiredArgsConstructor
@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private final MfaService mfaService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        LoginResponse loginResponse = mfaService.handleLoginSuccess(userDetails, request);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Result<LoginResponse> result = Result.success(loginResponse);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
