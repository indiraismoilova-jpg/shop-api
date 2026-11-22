package com.example.shop.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String websiteUrl;

    @Column(length = 100)
    private String country;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
