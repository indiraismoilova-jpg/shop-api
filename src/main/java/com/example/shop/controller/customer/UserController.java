package com.example.shop.controller.customer;

import com.example.shop.dto.request.ChangePasswordRequest;
import com.example.shop.dto.request.UpdateProfileRequest;
import com.example.shop.dto.response.UserResponse;
import com.example.shop.service.AuthService;
import com.example.shop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;
    private final AuthService authService;

    public UserController(
            UserService users,
            AuthService authService) {

        this.users = users;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserResponse getMe(Authentication authentication) {
        return users.getMe(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        return users.updateMe(
                authentication.getName(),
                request);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(
                authentication.getName(),
                request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            Authentication authentication) {

        users.deleteMe(authentication.getName());

        return ResponseEntity.noContent().build();
    }
}