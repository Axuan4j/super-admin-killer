package com.sak.service.filter;

import com.sak.service.handler.AuthenticationFailureHandlerImpl;
import com.sak.service.service.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CaptchaValidationFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;
    private final AuthenticationFailureHandlerImpl authenticationFailureHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/auth/login".equals(request.getServletPath()) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String captchaId = request.getParameter("captchaId");
            String captchaCode = request.getParameter("captcha");

            if (!captchaService.validateCaptcha(captchaId, captchaCode)) {
                authenticationFailureHandler.onAuthenticationFailure(
                        request,
                        response,
                        new BadCredentialsException("验证码错误或已过期")
                );
                return;
            }
        } catch (Exception exception) {
            authenticationFailureHandler.onAuthenticationFailure(
                    request,
                    response,
                    new AuthenticationServiceException("验证码校验失败，请稍后重试", exception)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
