package com.example.shop.dto.request;

import com.example.shop.model.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record PromoCodeRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[A-Za-z0-9_-]+")
        String code,

        @Size(max = 5000)
        String description,

        @NotNull
        DiscountType discountType,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal discountValue,

        @DecimalMin("0.00")
        BigDecimal minOrderAmount,

        @DecimalMin("0.00")
        BigDecimal maxDiscountAmount,

        @Min(1)
        Integer maxUsageCount,

        @Min(1)
        Integer maxUsagePerUser,

        @NotNull
        LocalDateTime validFrom,

        @NotNull
        LocalDateTime validTo,

        @NotNull
        Boolean isActive,

        Set<@NotNull @Positive Long> categoryIds,

        Set<@NotNull @Positive Long> productIds
) {
}