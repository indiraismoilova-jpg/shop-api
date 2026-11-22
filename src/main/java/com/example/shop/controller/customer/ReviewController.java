package com.example.shop.controller.customer;

import com.example.shop.dto.request.ReviewRequest;
import com.example.shop.dto.response.ReviewResponse;
import com.example.shop.dto.response.ReviewSummaryResponse;
import com.example.shop.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public Page<ReviewResponse> list(
            @PathVariable Long productId,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        return reviews.list(productId, pageable);
    }

    @GetMapping("/summary")
    public ReviewSummaryResponse summary(
            @PathVariable Long productId) {

        return reviews.summary(productId);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> create(
            @PathVariable Long productId,
            Authentication authentication,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse created = reviews.create(
                productId,
                authentication.getName(),
                request);

        return ResponseEntity.status(201)
                .header(
                        "Location",
                        "/api/products/" + productId
                                + "/reviews/" + created.id())
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse update(
            @PathVariable Long productId,
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ReviewRequest request) {

        return reviews.update(
                productId,
                id,
                authentication.getName(),
                request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long productId,
            @PathVariable Long id,
            Authentication authentication) {

        reviews.delete(
                productId,
                id,
                authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewResponse approve(
            @PathVariable Long productId,
            @PathVariable Long id) {

        return reviews.approve(productId, id);
    }
}