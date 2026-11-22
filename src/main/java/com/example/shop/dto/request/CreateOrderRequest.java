package com.example.shop.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull
        @Positive
        Long addressId,

        @NotNull
        @Positive
        Long deliveryOptionId,

        @Size(max = 5000)
        String customerNote
) {
}