package com.example.shop.dto.response;

import com.example.shop.model.enums.DeliveryMethod;
import com.example.shop.model.enums.OrderStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        ShippingAddressResponse shippingAddress,
        DeliveryMethod deliveryMethod,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal discount,
        BigDecimal totalAmount,
        String promoCode,
        String customerNote,
        String adminNote,
        String trackingNumber,
        LocalDateTime estimatedDeliveryDate,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        Page<OrderItemResponse> items
) {
}