package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sak.service.dto.LoginResponse;
import com.sak.service.dto.MfaCodeRequest;
import com.sak.service.dto.MfaLoginVerifyRequest;
import com.sak.service.dto.MfaSetupResponse;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.CustomUserDetailsService;
import com.sak.service.service.LoginLogService;
import com.sak.service.service.MfaService;
import com.sak.service.service.TokenService;
import com.sak.service.util.Base32Util;
import com.sak.service.util.QrCodeUtil;
import com.sak.service.util.TotpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private static final String SETUP_KEY_PREFIX = "auth:mfa:setup:";
    private static final String LOGIN_CHALLENGE_KEY_PREFIX = "auth:mfa:challenge:";
    private static final Duration SETUP_EXPIRE = Duration.ofMinutes(10);
    private static final Duration LOGIN_CHALLENGE_EXPIRE = Duration.ofMinutes(5);
    private static final String ISSUER = "Superkiller Admin";

    private final SysUserMapper sysUserMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenService tokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final LoginLogService loginLogService;
    private final QrCodeUtil qrCodeUtil;

    @Override
    public LoginResponse handleLoginSuccess(UserDetails userDetails, HttpServletRequest request) {
        SysUser user = getRequiredUser(userDetails.getUsername());
        if (isMfaEnabled(user)) {
            String challengeToken = UUID.randomUUID().toString().replace("-", "");
            redisTemplate.opsForValue().set(LOGIN_CHALLENGE_KEY_PREFIX + challengeToken, user.getUsername(), LOGIN_CHALLENGE_EXPIRE);
            return LoginResponse.mfaChallenge(user.getUsername(), challengeToken);
        }

        Map<String, String> tokens = tokenService.generateTokenAndCache(userDetails, request);
        loginLogService.recordSuccess(userDetails.getUsername(), request);
        return LoginResponse.loginSuccess(tokens.get("accessToken"), tokens.get("refreshToken"), userDetails.getUsername());
    }

    @Override
    public LoginResponse verifyLoginCode(MfaLoginVerifyRequest request, HttpServletRequest servletRequest) {
        String challengeToken = request.getChallengeToken();
        Object usernameObject = redisTemplate.opsForValue().get(LOGIN_CHALLENGE_KEY_PREFIX + challengeToken);
        if (usernameObject == null) {
            throw new IllegalArgumentException("二级验证已过期，请重新登录");
        }

        String username = String.valueOf(usernameObject);
        SysUser user = getRequiredUser(username);
        if (!isMfaEnabled(user)) {
            redisTemplate.delete(LOGIN_CHALLENGE_KEY_PREFIX + challengeToken);
            throw new IllegalStateException("当前账号未启用二级验证");
        }
        if (!TotpUtil.verifyCode(user.getMfaSecret(), request.getCode(), 1)) {
            loginLogService.recordFailure(username, "二级验证失败：动态验证码错误", servletRequest);
            throw new IllegalArgumentException("动态验证码错误");
        }

        redisTemplate.delete(LOGIN_CHALLENGE_KEY_PREFIX + challengeToken);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        Map<String, String> tokens = tokenService.generateTokenAndCache(userDetails, servletRequest);
        loginLogService.recordSuccess(username, servletRequest);
        return LoginResponse.loginSuccess(tokens.get("accessToken"), tokens.get("refreshToken"), username);
    }

    @Override
    public MfaSetupResponse createSetup(String username) {
        SysUser user = getRequiredUser(username);
        String secret = Base32Util.generateSecret(20);
        redisTemplate.opsForValue().set(SETUP_KEY_PREFIX + username, secret, SETUP_EXPIRE);

        String accountName = user.getUsername();
        String otpauthUri = buildOtpAuthUri(accountName, secret);
        String qrCodeBase64 = qrCodeUtil.toBase64Png(otpauthUri, 240);
        return new MfaSetupResponse(ISSUER, accountName, secret, otpauthUri, qrCodeBase64);
    }

    @Override
    @Transactional
    public void enableMfa(String username, MfaCodeRequest request) {
        String secret = getPendingSetupSecret(username);
        if (!TotpUtil.verifyCode(secret, request.getCode(), 1)) {
            throw new IllegalArgumentException("动态验证码错误");
        }
        SysUser user = getRequiredUser(username);
        user.setMfaEnabled(1);
        user.setMfaSecret(secret);
        user.setMfaBoundAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        redisTemplate.delete(SETUP_KEY_PREFIX + username);
    }

    @Override
    @Transactional
    public void disableMfa(String username, MfaCodeRequest request) {
        SysUser user = getRequiredUser(username);
        if (!isMfaEnabled(user)) {
            return;
        }
        if (!TotpUtil.verifyCode(user.getMfaSecret(), request.getCode(), 1)) {
            throw new IllegalArgumentException("动态验证码错误");
        }
        user.setMfaEnabled(0);
        user.setMfaSecret(null);
        user.setMfaBoundAt(null);
        sysUserMapper.updateById(user);
        redisTemplate.delete(SETUP_KEY_PREFIX + username);
    }

    private String getPendingSetupSecret(String username) {
        Object secret = redisTemplate.opsForValue().get(SETUP_KEY_PREFIX + username);
        if (secret == null || !StringUtils.hasText(String.valueOf(secret))) {
            throw new IllegalArgumentException("绑定信息已过期，请重新生成二维码");
        }
        return String.valueOf(secret);
    }

    private boolean isMfaEnabled(SysUser user) {
        return user != null
                && Integer.valueOf(1).equals(user.getMfaEnabled())
                && StringUtils.hasText(user.getMfaSecret());
    }

    private SysUser getRequiredUser(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private String buildOtpAuthUri(String accountName, String secret) {
        String label = urlEncode(ISSUER) + ":" + urlEncode(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + urlEncode(ISSUER)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
