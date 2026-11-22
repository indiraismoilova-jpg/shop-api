package com.example.shop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
        @NotNull
        @Positive
        Long productId,

        @Positive
        Long variantId,

        @NotNull
        @Min(1)
        @Max(10000)
        Integer quantity
) {
}