package com.example.shop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DeliveryQuoteResponse(
        Long deliveryOptionId,
        BigDecimal shippingCost,
        int estimatedDaysMin,
        int estimatedDaysMax,
        LocalDate estimatedDeliveryDateMin,
        LocalDate estimatedDeliveryDateMax
) {
}