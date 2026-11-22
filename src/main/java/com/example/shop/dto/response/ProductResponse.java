package com.example.shop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        String shortDescription,
        String sku,
        BigDecimal basePrice,
        BigDecimal discountPrice,
        LocalDateTime discountStartDate,
        LocalDateTime discountEndDate,
        Integer stockQuantity,
        Integer lowStockThreshold,
        BigDecimal weight,
        String dimensions,
        boolean isActive,
        boolean isFeatured,
        boolean isNewArrival,
        BigDecimal averageRating,
        Integer reviewCount,
        Long viewCount,
        Long salesCount,
        String metaTitle,
        String metaDescription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        com.example.shop.dto.response.CategoryResponse category,
        com.example.shop.dto.response.BrandResponse brand,
        List<com.example.shop.dto.response.ProductImageResponse> images,
        List<com.example.shop.dto.response.ProductSpecificationResponse> specifications
) {
}