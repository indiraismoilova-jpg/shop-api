package com.example.shop.repository;

import com.example.shop.model.entity.DeliveryOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryOptionRepository
        extends JpaRepository<DeliveryOption, Long> {

    Page<DeliveryOption> findByIsActiveTrue(Pageable pageable);
}