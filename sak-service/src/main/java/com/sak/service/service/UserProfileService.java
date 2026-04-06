package com.sak.service.service;

import com.sak.service.dto.UserInfoResponse;
import com.sak.service.dto.UserPasswordUpdateRequest;
import com.sak.service.dto.UserProfileUpdateRequest;

public interface UserProfileService {
    UserInfoResponse getUserInfo(String username);

    UserInfoResponse updateUserProfile(String username, UserProfileUpdateRequest request);

    void updatePassword(String username, UserPasswordUpdateRequest request);
}
