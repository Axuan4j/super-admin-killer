package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.config.StaticResourceConfig;
import com.sak.service.dto.AvatarUploadResponse;
import com.sak.service.dto.UserPasswordUpdateRequest;
import com.sak.service.dto.UserInfoResponse;
import com.sak.service.dto.UserProfileUpdateRequest;
import com.sak.service.service.TokenService;
import com.sak.service.service.UserProfileService;
import com.sak.service.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;
    private final JwtUtils jwtUtils;
    private final UserProfileService userProfileService;
    private final StaticResourceConfig staticResourceConfig;

    @GetMapping("/info")
    public Result<UserInfoResponse> getCurrentUser(Authentication authentication) {
        return Result.success(userProfileService.getUserInfo(authentication.getName()));
    }

    @PutMapping("/profile")
    public Result<UserInfoResponse> updateCurrentUserProfile(Authentication authentication, @RequestBody UserProfileUpdateRequest request) {
        return Result.success(userProfileService.updateUserProfile(authentication.getName(), request));
    }

    @PutMapping("/password")
    public Result<Void> updateCurrentUserPassword(Authentication authentication, @RequestBody UserPasswordUpdateRequest request) {
        userProfileService.updatePassword(authentication.getName(), request);
        return Result.success();
    }

    @PostMapping("/avatar")
    public Result<AvatarUploadResponse> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择头像文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持上传图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error("头像文件大小不能超过 2MB");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String fileName = authentication.getName() + "-" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path avatarDir = staticResourceConfig.getAvatarStorageDir();
        Files.createDirectories(avatarDir);
        Files.copy(file.getInputStream(), avatarDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        return Result.success(new AvatarUploadResponse(staticResourceConfig.getAvatarAccessPrefix() + fileName, fileName));
    }

    @PostMapping("/refresh")
    public Result<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.error("refreshToken is required");
        }

        // 验证 refresh_token
        if (!jwtUtils.validateToken(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
            return Result.error("invalid refreshToken");
        }

        // 检查 refresh_token 是否在黑名单/已撤销
        if (!tokenService.isRefreshTokenValid(refreshToken)) {
            return Result.error("refreshToken has been revoked");
        }

        // 生成新的 access_token
        String newAccessToken = tokenService.refreshAccessToken(refreshToken);
        if (newAccessToken == null) {
            return Result.error("refresh failed");
        }

        return Result.success(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody Map<String, String> request) {
        String accessToken = request.get("accessToken");
        String refreshToken = request.get("refreshToken");
        String token = request.get("token");

        if (accessToken != null && !accessToken.isEmpty()) {
            tokenService.deleteToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            tokenService.deleteToken(refreshToken);
        }
        if (token != null && !token.isEmpty()) {
            tokenService.deleteToken(token);
        }
        return Result.success(null);
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}
