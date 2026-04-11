package com.sak.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.dto.OnlineUserSessionResponse;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TokenService {

    private static final String DEVICE_TYPE_HEADER = "X-Client-Device";
    private static final String ACCESS_TOKEN_PREFIX = "auth:access:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String USER_PREFIX = "auth:user:";
    private static final String SESSION_PREFIX = "auth:session:";
    private static final String USER_SESSIONS_PREFIX = "auth:user:sessions:";
    private static final String ONLINE_SESSION_SET_KEY = "auth:sessions:online";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    @Value("${jwt.access-token-expiration:10m}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:7d}")
    private Duration refreshTokenExpiration;

    public TokenService(
            RedisTemplate<String, Object> redisTemplate,
            JwtUtils jwtUtils,
            SysUserMapper sysUserMapper,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.jwtUtils = jwtUtils;
        this.sysUserMapper = sysUserMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, String> generateTokenAndCache(UserDetails userDetails, HttpServletRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, userDetails.getUsername())
                .last("limit 1"));
        String deviceType = resolveDeviceType(request);
        invalidateExistingSessions(user, userDetails.getUsername(), deviceType);

        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtUtils.generateAccessToken(userDetails.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());
        String currentTime = now();

        redisTemplate.opsForValue().set(accessKey(accessToken), sessionId, accessTokenExpiration);
        redisTemplate.opsForValue().set(refreshKey(refreshToken), sessionId, refreshTokenExpiration);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", userDetails.getUsername());
        userInfo.put("authorities", userDetails.getAuthorities());
        redisTemplate.opsForHash().putAll(USER_PREFIX + userDetails.getUsername(), userInfo);
        redisTemplate.expire(USER_PREFIX + userDetails.getUsername(), refreshTokenExpiration);

        Map<String, String> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", sessionId);
        sessionInfo.put("userId", user == null || user.getId() == null ? "" : String.valueOf(user.getId()));
        sessionInfo.put("username", userDetails.getUsername());
        sessionInfo.put("nickName", user == null ? userDetails.getUsername() : defaultString(user.getNickName(), userDetails.getUsername()));
        sessionInfo.put("ip", resolveClientIp(request));
        sessionInfo.put("userAgent", resolveUserAgent(request));
        sessionInfo.put("deviceType", deviceType);
        sessionInfo.put("loginTime", currentTime);
        sessionInfo.put("lastActiveTime", currentTime);
        sessionInfo.put("accessToken", accessToken);
        sessionInfo.put("refreshToken", refreshToken);
        redisTemplate.opsForHash().putAll(sessionKey(sessionId), sessionInfo);
        redisTemplate.expire(sessionKey(sessionId), refreshTokenExpiration);
        redisTemplate.opsForSet().add(ONLINE_SESSION_SET_KEY, sessionId);
        if (user != null && user.getId() != null) {
            String userSessionsKey = USER_SESSIONS_PREFIX + user.getId();
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, refreshTokenExpiration);
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("sessionId", sessionId);
        return tokens;
    }

    private void invalidateExistingSessions(SysUser user, String username, String deviceType) {
        if (user == null || user.getId() == null) {
            redisTemplate.delete(USER_PREFIX + username);
            return;
        }

        String userSessionsKey = USER_SESSIONS_PREFIX + user.getId();
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        if (sessionIds == null || sessionIds.isEmpty()) {
            redisTemplate.delete(USER_PREFIX + username);
            return;
        }

        for (Object rawSessionId : sessionIds) {
            if (rawSessionId == null) {
                continue;
            }
            String sessionId = String.valueOf(rawSessionId);
            if (StringUtils.hasText(sessionId)) {
                Map<String, String> sessionInfo = getSessionInfo(sessionId);
                if (sessionInfo.isEmpty()) {
                    redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
                    continue;
                }
                String sessionDeviceType = sessionInfo.get("deviceType");
                if (deviceType.equals(sessionDeviceType)) {
                    deleteSession(sessionId);
                }
            }
        }
    }

    public String extractUsernameFromToken(String token) {
        String sessionId = getSessionIdByAccessToken(token);
        if (!StringUtils.hasText(sessionId)) {
            sessionId = getSessionIdByRefreshToken(token);
        }
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        Object username = redisTemplate.opsForHash().get(sessionKey(sessionId), "username");
        return username == null ? null : String.valueOf(username);
    }

    public Map<String, Object> getUserInfo(String username) {
        String userKey = USER_PREFIX + username;
        HashOperations<String, String, Object> opsForHash = redisTemplate.opsForHash();
        return opsForHash.entries(userKey);
    }

    public void deleteToken(String token) {
        String sessionId = getSessionIdByAccessToken(token);
        if (!StringUtils.hasText(sessionId)) {
            sessionId = getSessionIdByRefreshToken(token);
        }
        if (StringUtils.hasText(sessionId)) {
            deleteSession(sessionId);
        }
    }

    public void deleteSession(String sessionId) {
        Map<String, String> sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo.isEmpty()) {
            redisTemplate.opsForSet().remove(ONLINE_SESSION_SET_KEY, sessionId);
            return;
        }

        String accessToken = sessionInfo.get("accessToken");
        String refreshToken = sessionInfo.get("refreshToken");
        String userId = sessionInfo.get("userId");
        String username = sessionInfo.get("username");

        if (StringUtils.hasText(accessToken)) {
            redisTemplate.delete(accessKey(accessToken));
        }
        if (StringUtils.hasText(refreshToken)) {
            redisTemplate.delete(refreshKey(refreshToken));
        }

        redisTemplate.delete(sessionKey(sessionId));
        redisTemplate.opsForSet().remove(ONLINE_SESSION_SET_KEY, sessionId);

        if (StringUtils.hasText(userId)) {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            Long remain = redisTemplate.opsForSet().size(userSessionsKey);
            if (remain == null || remain == 0) {
                redisTemplate.delete(userSessionsKey);
                if (StringUtils.hasText(username)) {
                    redisTemplate.delete(USER_PREFIX + username);
                }
            }
        }
    }

    public boolean isAccessTokenValid(String token) {
        String sessionId = getSessionIdByAccessToken(token);
        return StringUtils.hasText(sessionId) && redisTemplate.hasKey(sessionKey(sessionId));
    }

    public boolean isRefreshTokenValid(String token) {
        String sessionId = getSessionIdByRefreshToken(token);
        return StringUtils.hasText(sessionId) && redisTemplate.hasKey(sessionKey(sessionId));
    }

    public Map<String, String> refreshTokens(String refreshToken) {
        String sessionId = getSessionIdByRefreshToken(refreshToken);
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        Map<String, String> sessionInfo = getSessionInfo(sessionId);
        String username = sessionInfo.get("username");
        if (!StringUtils.hasText(username)) {
            deleteSession(sessionId);
            return null;
        }

        String oldAccessToken = sessionInfo.get("accessToken");
        String oldRefreshToken = sessionInfo.get("refreshToken");
        String newAccessToken = jwtUtils.generateAccessToken(username);
        String newRefreshToken = jwtUtils.generateRefreshToken(username);

        if (StringUtils.hasText(oldAccessToken)) {
            redisTemplate.delete(accessKey(oldAccessToken));
        }
        if (StringUtils.hasText(oldRefreshToken)) {
            redisTemplate.delete(refreshKey(oldRefreshToken));
        }
        redisTemplate.opsForValue().set(accessKey(newAccessToken), sessionId, accessTokenExpiration);
        redisTemplate.opsForValue().set(refreshKey(newRefreshToken), sessionId, refreshTokenExpiration);
        redisTemplate.opsForHash().put(sessionKey(sessionId), "accessToken", newAccessToken);
        redisTemplate.opsForHash().put(sessionKey(sessionId), "refreshToken", newRefreshToken);
        redisTemplate.opsForHash().put(sessionKey(sessionId), "lastActiveTime", now());
        extendSessionLifetime(sessionId, sessionInfo);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        return tokens;
    }

    public String getSessionIdByAccessToken(String token) {
        Object sessionId = redisTemplate.opsForValue().get(accessKey(token));
        return sessionId == null ? null : String.valueOf(sessionId);
    }

    public String getSessionIdByRefreshToken(String token) {
        Object sessionId = redisTemplate.opsForValue().get(refreshKey(token));
        return sessionId == null ? null : String.valueOf(sessionId);
    }

    public void touchSessionByAccessToken(String token, HttpServletRequest request) {
        String sessionId = getSessionIdByAccessToken(token);
        if (StringUtils.hasText(sessionId)) {
            touchSession(sessionId, request);
        }
    }

    public void touchSessionBySessionId(String sessionId) {
        touchSession(sessionId, null);
    }

    public boolean hasSession(String sessionId) {
        return redisTemplate.hasKey(sessionKey(sessionId));
    }

    public List<OnlineUserSessionResponse> listOnlineSessions() {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(ONLINE_SESSION_SET_KEY);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<OnlineUserSessionResponse> sessions = new ArrayList<>();
        for (Object rawSessionId : sessionIds) {
            String sessionId = rawSessionId == null ? null : String.valueOf(rawSessionId);
            if (!StringUtils.hasText(sessionId)) {
                continue;
            }

            Map<String, String> sessionInfo = getSessionInfo(sessionId);
            if (sessionInfo.isEmpty()) {
                redisTemplate.opsForSet().remove(ONLINE_SESSION_SET_KEY, sessionId);
                continue;
            }

            OnlineUserSessionResponse response = new OnlineUserSessionResponse();
            response.setSessionId(sessionId);
            response.setUserId(parseLong(sessionInfo.get("userId")));
            response.setUsername(sessionInfo.get("username"));
            response.setNickName(sessionInfo.get("nickName"));
            response.setIp(sessionInfo.get("ip"));
            response.setUserAgent(sessionInfo.get("userAgent"));
            response.setLoginTime(sessionInfo.get("loginTime"));
            response.setLastActiveTime(sessionInfo.get("lastActiveTime"));
            sessions.add(response);
        }

        sessions.sort((left, right) -> defaultString(right.getLastActiveTime(), "").compareTo(defaultString(left.getLastActiveTime(), "")));
        return sessions;
    }

    private void touchSession(String sessionId, HttpServletRequest request) {
        if (!redisTemplate.hasKey(sessionKey(sessionId))) {
            return;
        }
        Map<String, String> sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().put(sessionKey(sessionId), "lastActiveTime", now());
        if (request != null) {
            redisTemplate.opsForHash().put(sessionKey(sessionId), "ip", resolveClientIp(request));
            redisTemplate.opsForHash().put(sessionKey(sessionId), "userAgent", resolveUserAgent(request));
        }
        extendSessionLifetime(sessionId, sessionInfo);
    }

    private void extendSessionLifetime(String sessionId, Map<String, String> sessionInfo) {
        redisTemplate.expire(sessionKey(sessionId), refreshTokenExpiration);

        String username = sessionInfo.get("username");
        if (StringUtils.hasText(username)) {
            redisTemplate.expire(USER_PREFIX + username, refreshTokenExpiration);
        }

        String userId = sessionInfo.get("userId");
        if (StringUtils.hasText(userId)) {
            redisTemplate.expire(USER_SESSIONS_PREFIX + userId, refreshTokenExpiration);
        }
    }

    private Map<String, String> getSessionInfo(String sessionId) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (raw.isEmpty()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(raw, new TypeReference<>() {});
    }

    private String accessKey(String token) {
        return ACCESS_TOKEN_PREFIX + token;
    }

    private String refreshKey(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return defaultString(request.getRemoteAddr(), "");
    }

    private String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return defaultString(request.getHeader("User-Agent"), "");
    }

    private String resolveDeviceType(HttpServletRequest request) {
        if (request == null) {
            return "web";
        }
        String deviceType = defaultString(request.getHeader(DEVICE_TYPE_HEADER), "").trim().toLowerCase();
        if (!StringUtils.hasText(deviceType)) {
            return "web";
        }
        return deviceType;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
