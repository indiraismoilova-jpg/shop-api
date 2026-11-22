package com.example.shop.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        Integer sortOrder,
        boolean isActive,
        Long parentCategoryId
) {
}