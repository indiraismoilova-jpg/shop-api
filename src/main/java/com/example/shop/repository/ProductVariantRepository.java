package com.example.shop.repository;

import com.example.shop.model.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    Page<ProductVariant> findByProductIdAndIsActiveTrue(
            Long productId,
            Pageable pageable);

    boolean existsBySkuAndIdNot(
            String sku,
            Long id);

    boolean existsBySku(String sku);
}