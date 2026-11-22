package com.example.shop.dto.request;

import jakarta.validation.constraints.Size;

public record OrderNoteRequest(
        @Size(max = 5000)
        String note
) {
}