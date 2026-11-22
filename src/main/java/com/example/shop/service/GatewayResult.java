package com.example.shop.service;

public record GatewayResult(
        boolean approved,
        String transactionId,
        String lastFourDigits,
        String errorCode,
        String errorMessage
) {
}