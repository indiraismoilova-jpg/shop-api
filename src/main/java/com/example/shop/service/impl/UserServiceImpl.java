package com.example.shop.service.impl;

import com.example.shop.dto.request.UpdateProfileRequest;
import com.example.shop.dto.response.UserResponse;
import com.example.shop.exception.UnauthorizedException;
import com.example.shop.mapper.UserMapper;
import com.example.shop.model.entity.User;
import com.example.shop.repository.RefreshTokenRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final UserMapper mapper;
    private final RefreshTokenRepository refreshTokens;

    public UserServiceImpl(
            UserRepository users,
            UserMapper mapper,
            RefreshTokenRepository refreshTokens) {

        this.users = users;
        this.mapper = mapper;
        this.refreshTokens = refreshTokens;
    }

    @Override
    public UserResponse getMe(String email) {
        return mapper.toResponse(findCurrentUser(email));
    }

    @Override
    @Transactional
    public UserResponse updateMe(
            String email,
            UpdateProfileRequest request) {

        User user = findCurrentUser(email);

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone() == null
                ? null
                : request.phone().trim());

        users.saveAndFlush(user);

        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    public void deleteMe(String email) {
        User user = findCurrentUser(email);

        user.setEnabled(false);
        users.saveAndFlush(user);

        refreshTokens.revokeAllByUserId(user.getId());
    }

    private User findCurrentUser(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User account is unavailable"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException(
                    "User account is disabled");
        }

        return user;
    }
}