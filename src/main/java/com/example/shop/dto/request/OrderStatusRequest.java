package com.example.shop.dto.request;

import com.example.shop.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @NotNull
        OrderStatus status
) {
}