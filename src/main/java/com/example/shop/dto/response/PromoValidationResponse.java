package com.example.shop.dto.response;

import java.math.BigDecimal;

public record PromoValidationResponse(
        String code,
        boolean valid,
        BigDecimal discount,
        BigDecimal shippingDiscount
) {
}