package com.sak.service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.common.Result;
import com.sak.service.dto.LoginResponse;
import com.sak.service.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Map<String, String> tokens = tokenService.generateTokenAndCache(userDetails, request);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Result<LoginResponse> result = Result.success(new LoginResponse(
                tokens.get("accessToken"),
                tokens.get("refreshToken"),
                userDetails.getUsername()
        ));
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
