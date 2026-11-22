package com.example.shop.dto.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public record CartResponse(
        Long id,
        Page<CartItemResponse> items,
        int totalQuantity,
        BigDecimal subtotal,
        String promoCode,
        BigDecimal discount,
        BigDecimal total
) {
}