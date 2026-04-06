package com.superkiller.backend.service;

import com.superkiller.backend.dto.UserInfoResponse;
import com.superkiller.backend.dto.UserPasswordUpdateRequest;
import com.superkiller.backend.dto.UserProfileUpdateRequest;

public interface UserProfileService {
    UserInfoResponse getUserInfo(String username);

    UserInfoResponse updateUserProfile(String username, UserProfileUpdateRequest request);

    void updatePassword(String username, UserPasswordUpdateRequest request);
}
