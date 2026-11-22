package com.example.shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBrandRequest(

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
        String logoUrl,

        @Size(max = 500)
        String websiteUrl,

        @Size(max = 100)
        String country,

        @NotNull
        Boolean isActive
) {
}