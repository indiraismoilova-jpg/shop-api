package com.example.shop.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record ReviewSummaryResponse(
        Long productId,
        long reviewCount,
        BigDecimal averageRating,
        Map<Integer, Long> ratingCounts
) {
}