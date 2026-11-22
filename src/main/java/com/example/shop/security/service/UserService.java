package com.example.shop.service;

import com.example.shop.dto.request.UpdateProfileRequest;
import com.example.shop.dto.response.UserResponse;

public interface UserService {

    UserResponse getMe(String email);

    UserResponse updateMe(
            String email,
            UpdateProfileRequest request);

    void deleteMe(String email);
}