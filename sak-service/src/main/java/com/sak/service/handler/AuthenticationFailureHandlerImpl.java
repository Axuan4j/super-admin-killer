package com.sak.service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.common.Result;
import com.sak.service.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final LoginLogService loginLogService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        loginLogService.recordFailure(request.getParameter("username"), "登录失败：" + exception.getMessage(), request);
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Result<?> result = Result.unauthorized("登录失败：" + exception.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
