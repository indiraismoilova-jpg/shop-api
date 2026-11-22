package com.example.shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(

        @NotBlank
        @Size(max = 512)
        String refreshToken
) {
}
