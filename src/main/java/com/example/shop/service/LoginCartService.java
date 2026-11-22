package com.example.shop.service;

import com.example.shop.dto.request.LoginRequest;
import com.example.shop.dto.request.MergeCartRequest;
import com.example.shop.dto.response.AuthResponse;
import com.example.shop.security.jwt.JwtTokenProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginCartService {

    private final AuthService authService;
    private final CartService carts;
    private final JwtTokenProvider tokens;

    public LoginCartService(
            AuthService authService,
            CartService carts,
            JwtTokenProvider tokens) {

        this.authService = authService;
        this.carts = carts;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AuthResponse response = authService.login(request);

        if (request.guestItems() != null
                && !request.guestItems().isEmpty()) {

            String email = tokens.getEmail(
                    response.accessToken());

            carts.merge(
                    email,
                    new MergeCartRequest(request.guestItems()),
                    PageRequest.of(0, 20));
        }

        return response;
    }
}