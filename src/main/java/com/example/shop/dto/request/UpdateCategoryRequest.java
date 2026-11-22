package com.example.shop.dto.request;

import jakarta.validation.constraints.*;

public record UpdateCategoryRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 150)
        @Pattern(
                regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "Use lowercase letters, digits and single hyphens")
        String slug,

        @Size(max = 10000)
        String description,

        @Size(max = 500)
        String imageUrl,

        @NotNull
        @PositiveOrZero
        Integer sortOrder,

        @NotNull
        Boolean isActive,

        @Positive
        Long parentCategoryId
) {
}