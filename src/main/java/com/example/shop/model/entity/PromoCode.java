package com.example.shop.model.entity;

import com.example.shop.model.enums.DiscountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "promo_codes",
        indexes = @Index(
                name = "idx_promo_created_by",
                columnList = "created_by_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    private Integer maxUsageCount;

    @Column(nullable = false)
    private Integer currentUsageCount = 0;

    private Integer maxUsagePerUser;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToMany
    @JoinTable(
            name = "promo_code_categories",
            joinColumns = @JoinColumn(name = "promo_code_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> applicableCategories = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "promo_code_products",
            joinColumns = @JoinColumn(name = "promo_code_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> applicableProducts = new HashSet<>();
}