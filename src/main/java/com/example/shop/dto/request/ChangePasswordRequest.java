package com.example.shop.dto.request;

import com.example.shop.validator.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank
        @Size(max = 72)
        String currentPassword,

        @NotBlank
        @ValidPassword
        String newPassword
) {
}