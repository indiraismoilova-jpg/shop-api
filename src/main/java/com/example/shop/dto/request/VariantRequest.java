package com.example.shop.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record VariantRequest(
        @NotBlank
        @Size(max = 64)
        String sku,

        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull
        @Min(0)
        Integer stockQuantity,

        @Size(max = 500)
        String imageUrl,

        Map<String, String> attributes,

        @NotNull
        Boolean isActive
) {
}