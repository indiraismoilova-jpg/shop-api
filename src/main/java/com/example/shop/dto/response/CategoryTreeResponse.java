package com.example.shop.dto.response;

import java.util.List;

public record CategoryTreeResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        Integer sortOrder,
        List<CategoryTreeResponse> children
) {
}