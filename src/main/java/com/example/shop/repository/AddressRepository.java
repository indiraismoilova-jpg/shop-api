package com.example.shop.repository;

import com.example.shop.model.entity.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AddressRepository
        extends JpaRepository<Address, Long> {

    Page<Address> findByUser_Id(
            Long userId,
            Pageable pageable);

    Optional<Address> findByIdAndUser_Id(
            Long id,
            Long userId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Address a
            SET a.isDefault = false
            WHERE a.user.id = :userId
            AND a.isDefault = true
            AND (:exceptId IS NULL OR a.id <> :exceptId)
            """)
    int clearOtherDefaults(
            @Param("userId") Long userId,
            @Param("exceptId") Long exceptId);
}