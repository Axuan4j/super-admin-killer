package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.CaptchaResponse;
import com.sak.service.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public Result<CaptchaResponse> getCaptcha() {
        return Result.success(captchaService.generateCaptcha());
    }
}
