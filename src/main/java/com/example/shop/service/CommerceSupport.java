package com.example.shop.service;

import com.example.shop.exception.InsufficientStockException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.exception.UnauthorizedException;
import com.example.shop.model.entity.Product;
import com.example.shop.model.entity.ProductVariant;
import com.example.shop.model.entity.User;
import com.example.shop.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class CommerceSupport {

    private final UserRepository users;
    private final EntityManager entityManager;

    public CommerceSupport(
            UserRepository users,
            EntityManager entityManager) {

        this.users = users;
        this.entityManager = entityManager;
    }

    public User user(String email, boolean lock) {
        return (lock
                ? users.findByEmailForUpdate(email)
                : users.findByEmail(email))
                .orElseThrow(() ->
                        new UnauthorizedException("User not found"));
    }

    public <T> T find(Class<T> type, Long id) {
        T item = entityManager.find(type, id);

        if (item == null) {
            throw new ResourceNotFoundException(
                    type.getSimpleName() + " not found");
        }

        return item;
    }

    public <T> T lock(Class<T> type, Long id) {
        T item = entityManager.find(
                type,
                id,
                LockModeType.PESSIMISTIC_WRITE);

        if (item == null) {
            throw new ResourceNotFoundException(
                    type.getSimpleName() + " not found");
        }

        return item;
    }

    public Product activeProduct(Long id) {
        Product product = find(Product.class, id);

        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product not found");
        }

        return product;
    }

    public ProductVariant variant(Product product, Long id) {
        if (id == null) {
            return null;
        }

        ProductVariant variant = find(ProductVariant.class, id);

        if (!variant.getProduct().getId().equals(product.getId())
                || !variant.isActive()) {

            throw bad(
                    "Variant does not belong to this product or is inactive");
        }

        return variant;
    }

    public BigDecimal price(
            Product product,
            ProductVariant variant) {

        if (variant != null) {
            return variant.getPrice();
        }

        LocalDateTime now = LocalDateTime.now();

        boolean discountStarted =
                product.getDiscountStartDate() == null
                        || !now.isBefore(product.getDiscountStartDate());

        boolean discountNotEnded =
                product.getDiscountEndDate() == null
                        || now.isBefore(product.getDiscountEndDate());

        if (product.getDiscountPrice() != null
                && discountStarted
                && discountNotEnded) {

            return product.getDiscountPrice();
        }

        return product.getBasePrice();
    }

    public void stock(
            Product product,
            ProductVariant variant,
            int quantity) {

        int available = variant == null
                ? product.getStockQuantity()
                - product.getReservedQuantity()
                : variant.getStockQuantity()
                - variant.getReservedQuantity();

        if (quantity < 1 || quantity > available) {
            throw new InsufficientStockException(
                    "Insufficient stock for " + product.getName());
        }
    }

    public Pageable page(
            Pageable pageable,
            String... fields) {

        Set<String> allowed = Set.of(fields);

        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw bad(
                        "Unsupported sort field: " + order.getProperty());
            }
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort());
    }

    public static ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message);
    }

    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message);
    }
}