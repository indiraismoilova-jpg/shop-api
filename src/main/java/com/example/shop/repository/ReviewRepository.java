package com.example.shop.repository;

import com.example.shop.model.entity.Review;
import com.example.shop.model.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndStatus(
            Long productId,
            ReviewStatus status,
            Pageable pageable);

    Optional<Review> findByIdAndProductId(
            Long id,
            Long productId);

    @Query("""
            SELECT r.rating, COUNT(r)
            FROM Review r
            WHERE r.product.id = :productId
              AND r.status = :status
            GROUP BY r.rating
            """)
    List<Object[]> countRatings(
            @Param("productId") Long productId,
            @Param("status") ReviewStatus status);
}