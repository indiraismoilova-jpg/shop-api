package com.example.shop.service.impl;

import com.example.shop.dto.request.ReviewRequest;
import com.example.shop.dto.response.ReviewResponse;
import com.example.shop.dto.response.ReviewSummaryResponse;
import com.example.shop.exception.ForbiddenException;
import com.example.shop.exception.UnauthorizedException;
import com.example.shop.model.entity.Product;
import com.example.shop.model.entity.Review;
import com.example.shop.model.entity.User;
import com.example.shop.model.enums.ReviewStatus;
import com.example.shop.repository.ReviewRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.service.ReviewService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviews;
    private final UserRepository users;
    private final EntityManager entityManager;

    public ReviewServiceImpl(
            ReviewRepository reviews,
            UserRepository users,
            EntityManager entityManager) {

        this.reviews = reviews;
        this.users = users;
        this.entityManager = entityManager;
    }

    @Override
    public Page<ReviewResponse> list(
            Long productId,
            Pageable pageable) {

        findProduct(productId);

        return reviews.findByProductIdAndStatus(
                        productId,
                        ReviewStatus.APPROVED,
                        pageable)
                .map(this::toResponse);
    }

    @Override
    public ReviewSummaryResponse summary(Long productId) {
        findProduct(productId);
        return buildSummary(productId);
    }

    @Override
    @Transactional
    public ReviewResponse create(
            Long productId,
            String email,
            ReviewRequest request) {

        Product product = lockProduct(productId);
        User user = findUser(email);

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);

        applyRequest(review, request);

        review = reviews.saveAndFlush(review);

        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse update(
            Long productId,
            Long reviewId,
            String email,
            ReviewRequest request) {

        Product product = lockProduct(productId);
        Review review = findReview(productId, reviewId);

        checkOwner(review, email);
        applyRequest(review, request);

        reviews.flush();
        refreshRating(product);

        return toResponse(review);
    }

    @Override
    @Transactional
    public void delete(
            Long productId,
            Long reviewId,
            String email) {

        Product product = lockProduct(productId);
        Review review = findReview(productId, reviewId);

        checkOwner(review, email);

        reviews.delete(review);
        reviews.flush();

        refreshRating(product);
    }

    private Product findProduct(Long productId) {
        Product product = entityManager.find(
                Product.class,
                productId);

        if (product == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found");
        }

        return product;
    }

    private Product lockProduct(Long productId) {
        Product product = entityManager.find(
                Product.class,
                productId,
                LockModeType.PESSIMISTIC_WRITE);

        if (product == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found");
        }

        return product;
    }

    private User findUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User not found"));
    }

    private Review findReview(
            Long productId,
            Long reviewId) {

        return reviews.findByIdAndProductId(
                        reviewId,
                        productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Review not found"));
    }

    private void checkOwner(
            Review review,
            String email) {

        User user = findUser(email);

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(
                    "You can only change your own review");
        }
    }

    private void applyRequest(
            Review review,
            ReviewRequest request) {

        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());
        review.setPros(request.pros());
        review.setCons(request.cons());

        review.setImages(
                request.images() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(request.images()));

        review.setStatus(ReviewStatus.PENDING);
    }

    private ReviewSummaryResponse buildSummary(
            Long productId) {

        Map<Integer, Long> counts = new LinkedHashMap<>();

        for (int rating = 1; rating <= 5; rating++) {
            counts.put(rating, 0L);
        }

        long total = 0;
        long ratingSum = 0;

        for (Object[] row : reviews.countRatings(
                productId,
                ReviewStatus.APPROVED)) {

            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();

            counts.put(rating, count);

            total += count;
            ratingSum += rating * count;
        }

        BigDecimal average = total == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(ratingSum)
                .divide(
                        BigDecimal.valueOf(total),
                        2,
                        RoundingMode.HALF_UP);

        return new ReviewSummaryResponse(
                productId,
                total,
                average,
                counts);
    }

    private void refreshRating(Product product) {
        ReviewSummaryResponse summary =
                buildSummary(product.getId());

        product.setAverageRating(summary.averageRating());
        product.setReviewCount(
                Math.toIntExact(summary.reviewCount()));
    }

    private ReviewResponse toResponse(Review review) {
        User user = review.getUser();

        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getPros(),
                review.getCons(),
                review.getImages() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(review.getImages()),
                review.isVerifiedPurchase(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewResponse approve(
            Long productId,
            Long reviewId) {

        Product product = lockProduct(productId);
        Review review = findReview(productId, reviewId);

        review.setStatus(ReviewStatus.APPROVED);

        reviews.flush();
        refreshRating(product);

        return toResponse(review);
    }

}