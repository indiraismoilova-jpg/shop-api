package com.example.shop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReviewRequest(
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Size(max = 200)
        String title,

        @Size(max = 10000)
        String comment,

        @Size(max = 5000)
        String pros,

        @Size(max = 5000)
        String cons,

        @Size(max = 10)
        List<@NotNull @Size(max = 2000) String> images
) {
}