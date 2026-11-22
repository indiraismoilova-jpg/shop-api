package com.example.shop.service.impl;

import com.example.shop.dto.request.ChangePasswordRequest;
import com.example.shop.dto.request.LoginRequest;
import com.example.shop.dto.request.RefreshTokenRequest;
import com.example.shop.dto.request.RegisterRequest;
import com.example.shop.dto.response.AuthResponse;
import com.example.shop.model.entity.RefreshToken;
import com.example.shop.model.entity.User;
import com.example.shop.model.enums.UserRole;
import com.example.shop.repository.RefreshTokenRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.security.jwt.JwtTokenProvider;
import com.example.shop.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokens;

    public AuthServiceImpl(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokens) {

        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        checkPasswordLength(request.password());

        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setRole(UserRole.CUSTOMER);
        user.setEnabled(true);
        user.setEmailVerified(false);

        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account could not be created");
        }

        return createTokens(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        checkPasswordLength(request.password());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.password()));
        } catch (AuthenticationException e) {
            throw unauthorized();
        }

        User user = users.findByEmail(email)
                .orElseThrow(this::unauthorized);

        user.setLastLoginAt(LocalDateTime.now());

        return createTokens(user);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!tokens.isValidRefreshToken(token)) {
            throw unauthorized();
        }

        RefreshToken stored = refreshTokens.findByToken(hash(token))
                .orElseThrow(this::unauthorized);

        if (stored.isRevoked()
                || !stored.getExpiryDate()
                .isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw unauthorized();
        }

        User user = stored.getUser();

        if (!user.isEnabled()
                || !user.getEmail().equals(tokens.getEmail(token))) {
            throw unauthorized();
        }

        stored.setRevoked(true);

        return createTokens(user);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokens.findByToken(hash(request.refreshToken()))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Override
    public void changePassword(
            String email,
            ChangePasswordRequest request) {

        User user = users.findByEmail(email)
                .orElseThrow(this::unauthorized);

        if (!user.isEnabled()) {
            throw unauthorized();
        }

        checkPasswordLength(request.currentPassword());
        checkPasswordLength(request.newPassword());

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect");
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different");
        }

        user.setPassword(
                passwordEncoder.encode(request.newPassword()));

        users.save(user);
        refreshTokens.revokeAllByUserId(user.getId());
    }

    private AuthResponse createTokens(User user) {
        String accessToken = tokens.generateAccessToken(user);
        String refreshToken = tokens.generateRefreshToken(user);

        RefreshToken stored = new RefreshToken();
        stored.setToken(hash(refreshToken));
        stored.setUser(user);
        stored.setRevoked(false);
        stored.setExpiryDate(
                LocalDateTime.ofInstant(
                        tokens.getExpiration(refreshToken),
                        ZoneOffset.UTC));

        refreshTokens.save(stored);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                tokens.getExpiration(accessToken));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void checkPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes");
        }
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", e);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials or refresh token");
    }
}