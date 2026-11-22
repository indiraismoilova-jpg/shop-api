package com.example.shop.dto.response;

import com.example.shop.model.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String authorName,
        Integer rating,
        String title,
        String comment,
        String pros,
        String cons,
        List<String> images,
        boolean verifiedPurchase,
        ReviewStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}