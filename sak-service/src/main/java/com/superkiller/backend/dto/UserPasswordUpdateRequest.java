package com.superkiller.backend.dto;

import lombok.Data;

@Data
public class UserPasswordUpdateRequest {
    private String oldPassword;
    private String newPassword;
}
