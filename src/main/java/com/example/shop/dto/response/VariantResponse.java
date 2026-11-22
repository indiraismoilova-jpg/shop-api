package com.example.shop.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record VariantResponse(
        Long id,
        Long productId,
        String sku,
        String name,
        BigDecimal price,
        int stockQuantity,
        int reservedQuantity,
        boolean isActive,
        String imageUrl,
        Map<String, String> attributes
) {
}