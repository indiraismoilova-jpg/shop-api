package com.example.shop.service;

import com.example.shop.model.enums.OrderStatus;

import java.util.Set;

public final class OrderStatusRules {

    private OrderStatusRules() {
    }

    public static boolean canTransition(
            OrderStatus current,
            OrderStatus next) {

        if (current == null || next == null) {
            return false;
        }

        Set<OrderStatus> allowed = switch (current) {
            case PENDING -> Set.of(
                    OrderStatus.CONFIRMED,
                    OrderStatus.CANCELLED);

            case CONFIRMED -> Set.of(
                    OrderStatus.PAID,
                    OrderStatus.CANCELLED);

            case PAID -> Set.of(
                    OrderStatus.PROCESSING,
                    OrderStatus.REFUNDED);

            case PROCESSING -> Set.of(
                    OrderStatus.SHIPPED,
                    OrderStatus.CANCELLED);

            case SHIPPED -> Set.of(
                    OrderStatus.IN_TRANSIT);

            case IN_TRANSIT -> Set.of(
                    OrderStatus.DELIVERED);

            case DELIVERED -> Set.of(
                    OrderStatus.REFUNDED);

            case CANCELLED, REFUNDED -> Set.of();
        };

        return allowed.contains(next);
    }
}