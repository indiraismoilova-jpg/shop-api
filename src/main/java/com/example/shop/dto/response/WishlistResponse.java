package com.example.shop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal priceAtAdd,
        BigDecimal currentPrice,
        LocalDateTime addedAt
) {
}