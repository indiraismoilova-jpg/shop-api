package com.example.shop.dto.response;

import java.math.BigDecimal;

public record DeliveryResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal basePrice,
        BigDecimal freeThreshold,
        int estimatedDaysMin,
        int estimatedDaysMax
) {
}