package com.example.shop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LoginRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        String password,

        @Size(max = 100)
        List<@NotNull @Valid CartItemRequest> guestItems
) {

        public LoginRequest(String email, String password) {
                this(email, password, null);
        }

        @Override
        public String toString() {
                return "LoginRequest[email=" + email
                        + ", password=REDACTED]";
        }
}