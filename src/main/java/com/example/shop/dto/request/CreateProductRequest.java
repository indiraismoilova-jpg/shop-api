package com.example.shop.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateProductRequest(
        @NotBlank
        @Size(min = 2, max = 200)
        String name,

        @NotBlank
        @Size(max = 250)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
        String slug,

        @Size(max = 10000)
        String description,

        @Size(max = 1000)
        String shortDescription,

        @NotBlank
        @Size(max = 64)
        String sku,

        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal basePrice,

        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal discountPrice,

        LocalDateTime discountStartDate,

        LocalDateTime discountEndDate,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity,

        @NotNull
        @PositiveOrZero
        Integer lowStockThreshold,

        @Positive
        @Digits(integer = 7, fraction = 3)
        BigDecimal weight,

        @Size(max = 100)
        String dimensions,

        @NotNull
        Boolean isActive,

        @NotNull
        Boolean isFeatured,

        @NotNull
        Boolean isNewArrival,

        @Size(max = 200)
        String metaTitle,

        @Size(max = 500)
        String metaDescription,

        @NotNull
        @Positive
        Long categoryId,

        @Positive
        Long brandId
) {
}