package com.example.shop.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        Long variantId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        BigDecimal priceAtAdd,
        boolean priceChanged
) {
}