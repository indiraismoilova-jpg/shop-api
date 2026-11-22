package com.example.shop.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "delivery_options")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal freeThreshold;

    @Column(nullable = false)
    private Integer estimatedDaysMin;

    @Column(nullable = false)
    private Integer estimatedDaysMax;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}