package com.example.shop.service;

import com.example.shop.dto.request.ReviewRequest;
import com.example.shop.dto.response.ReviewResponse;
import com.example.shop.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    Page<ReviewResponse> list(
            Long productId,
            Pageable pageable);

    ReviewSummaryResponse summary(Long productId);

    ReviewResponse create(
            Long productId,
            String email,
            ReviewRequest request);

    ReviewResponse update(
            Long productId,
            Long reviewId,
            String email,
            ReviewRequest request);

    void delete(
            Long productId,
            Long reviewId,
            String email);
    ReviewResponse approve(Long productId, Long reviewId);
}
