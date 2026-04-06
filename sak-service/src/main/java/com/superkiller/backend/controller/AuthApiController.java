package com.superkiller.backend.controller;

import com.superkiller.backend.common.Result;
import com.superkiller.backend.dto.CaptchaResponse;
import com.superkiller.backend.service.CaptchaService;
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
