package com.example.shop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRequest(
        @NotNull
        @Min(0)
        Integer stockQuantity
) {
}
