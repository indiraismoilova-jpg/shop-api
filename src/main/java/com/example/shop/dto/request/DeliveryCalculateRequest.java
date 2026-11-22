package com.example.shop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DeliveryCalculateRequest(
        @NotNull
        @Positive
        Long deliveryOptionId,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal subtotal
) {
}