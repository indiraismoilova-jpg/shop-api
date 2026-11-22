package com.example.shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TrackingRequest(
        @NotBlank
        @Size(max = 100)
        String trackingNumber,

        LocalDateTime estimatedDeliveryDate
) {
}