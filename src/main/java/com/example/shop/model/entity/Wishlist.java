package com.example.shop.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wishlists",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_wishlist_user_product",
                columnNames = {"user_id", "product_id"}
        ),
        indexes = {
                @Index(name = "idx_wishlist_user", columnList = "user_id"),
                @Index(name = "idx_wishlist_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAdd;

    @Column(nullable = false)
    private boolean notifyOnPriceDrop = false;

    @Column(nullable = false)
    private boolean notifyOnBackInStock = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @PrePersist
    void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
