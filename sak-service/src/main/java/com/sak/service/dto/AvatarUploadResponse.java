package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvatarUploadResponse {
    private String avatarUrl;
    private String fileName;
}
