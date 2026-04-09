package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.convert.RequestVoConverter;
import com.sak.service.dto.LoginResponse;
import com.sak.service.service.MfaService;
import com.sak.service.vo.MfaLoginVerifyVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/mfa")
@RequiredArgsConstructor
public class AuthMfaController {

    private final MfaService mfaService;
    private final RequestVoConverter requestVoMapper;

    @PostMapping("/verify")
    public Result<LoginResponse> verify(@Valid @RequestBody MfaLoginVerifyVO request, HttpServletRequest servletRequest) {
        return Result.success(mfaService.verifyLoginCode(requestVoMapper.toMfaLoginVerifyRequest(request), servletRequest));
    }
}
