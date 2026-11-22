package com.example.shop.controller;

import com.example.shop.dto.request.ChangePasswordRequest;
import com.example.shop.dto.request.LoginRequest;
import com.example.shop.dto.request.RefreshTokenRequest;
import com.example.shop.dto.request.RegisterRequest;
import com.example.shop.dto.response.AuthResponse;
import com.example.shop.service.AuthService;
import com.example.shop.service.LoginCartService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginCartService loginCartService;

    public AuthController(
            AuthService authService,
            LoginCartService loginCartService) {

        this.authService = authService;
        this.loginCartService = loginCartService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(loginCartService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(
                authentication.getName(),
                request);

        return ResponseEntity.noContent().build();
    }
}