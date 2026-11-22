package com.example.shop.service;

import com.example.shop.exception.InsufficientStockException;
import com.example.shop.model.entity.Order;
import com.example.shop.model.entity.OrderItem;
import com.example.shop.model.entity.Product;
import com.example.shop.model.entity.ProductVariant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class InventoryService {

    private final CommerceSupport support;

    public InventoryService(CommerceSupport support) {
        this.support = support;
    }

    public void reserve(Order order) {
        for (OrderItem item : sortedItems(order)) {
            Product product = support.lock(
                    Product.class,
                    item.getProduct().getId());

            ProductVariant variant = lockVariant(item);

            if (!product.isActive()) {
                throw new InsufficientStockException(
                        "Product is inactive: " + product.getName());
            }

            if (variant != null
                    && (!variant.isActive()
                    || !variant.getProduct().getId()
                    .equals(product.getId()))) {

                throw new InsufficientStockException(
                        "Product variant is inactive or invalid");
            }

            support.stock(
                    product,
                    variant,
                    item.getQuantity());

            if (variant == null) {
                product.setReservedQuantity(
                        product.getReservedQuantity()
                                + item.getQuantity());
            } else {
                variant.setReservedQuantity(
                        variant.getReservedQuantity()
                                + item.getQuantity());
            }
        }
    }

    public void release(Order order) {
        for (OrderItem item : sortedItems(order)) {
            support.lock(
                    Product.class,
                    item.getProduct().getId());

            ProductVariant variant = lockVariant(item);
            Product product = item.getProduct();

            int reserved = variant == null
                    ? product.getReservedQuantity()
                    : variant.getReservedQuantity();

            requireReservation(reserved, item.getQuantity());

            if (variant == null) {
                product.setReservedQuantity(
                        reserved - item.getQuantity());
            } else {
                variant.setReservedQuantity(
                        reserved - item.getQuantity());
            }
        }
    }

    public void commit(Order order) {
        for (OrderItem item : sortedItems(order)) {
            Product product = support.lock(
                    Product.class,
                    item.getProduct().getId());

            ProductVariant variant = lockVariant(item);

            int reserved = variant == null
                    ? product.getReservedQuantity()
                    : variant.getReservedQuantity();

            int stock = variant == null
                    ? product.getStockQuantity()
                    : variant.getStockQuantity();

            int quantity = item.getQuantity();

            requireReservation(reserved, quantity);

            if (stock < quantity) {
                throw new InsufficientStockException(
                        "Insufficient stock to complete the order");
            }

            if (variant == null) {
                product.setStockQuantity(stock - quantity);
                product.setReservedQuantity(reserved - quantity);
            } else {
                variant.setStockQuantity(stock - quantity);
                variant.setReservedQuantity(reserved - quantity);
            }
        }
    }

    public void restore(Order order) {
        for (OrderItem item : sortedItems(order)) {
            Product product = support.lock(
                    Product.class,
                    item.getProduct().getId());

            ProductVariant variant = lockVariant(item);

            if (variant == null) {
                product.setStockQuantity(
                        product.getStockQuantity()
                                + item.getQuantity());
            } else {
                variant.setStockQuantity(
                        variant.getStockQuantity()
                                + item.getQuantity());
            }
        }
    }

    private ProductVariant lockVariant(OrderItem item) {
        if (item.getVariant() == null) {
            return null;
        }

        return support.lock(
                ProductVariant.class,
                item.getVariant().getId());
    }

    private void requireReservation(
            int reserved,
            int quantity) {

        if (quantity < 1 || reserved < quantity) {
            throw new InsufficientStockException(
                    "Not enough reserved stock for this order");
        }
    }

    private List<OrderItem> sortedItems(Order order) {
        return order.getOrderItems()
                .stream()
                .sorted(Comparator
                        .comparing((OrderItem item) ->
                                item.getProduct().getId())
                        .thenComparing(item ->
                                item.getVariant() == null
                                        ? 0L
                                        : item.getVariant().getId()))
                .toList();
    }
}