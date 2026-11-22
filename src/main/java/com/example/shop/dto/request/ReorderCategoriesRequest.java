package com.example.shop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ReorderCategoriesRequest(

        @NotNull
        @Size(min = 1, max = 100)
        List<@NotNull @Valid Item> items
) {

    public record Item(

            @NotNull
            @Positive
            Long categoryId,

            @NotNull
            @PositiveOrZero
            Integer sortOrder
    ) {
    }
}