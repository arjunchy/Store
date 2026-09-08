package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.ReviewRequest;
import com.ecommerce.backend.dto.response.ReviewResponse;
import com.ecommerce.backend.service.review.ReviewService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "POST /api/reviews - Creating review for product {} by userId: {}",
                request.productId(),
                userId
        );

        try {
            ReviewResponse response =
                    reviewService.createReview(userId, request);

            log.info(
                    "POST /api/reviews - Successfully created review with id: {} for product {}",
                    response.id(),
                    request.productId()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "POST /api/reviews - Failed for product {} by userId: {} - {}",
                    request.productId(),
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "POST /api/reviews - Unexpected error for product {} by userId: {}",
                    request.productId(),
                    userId,
                    e
            );

            throw e;
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForProduct(
            @PathVariable String productId) {

        log.info(
                "GET /api/reviews/product/{} - Fetching reviews",
                productId
        );

        try {
            List<ReviewResponse> reviews =
                    reviewService.getReviewsForProduct(productId);

            log.debug(
                    "GET /api/reviews/product/{} - Retrieved {} reviews",
                    productId,
                    reviews.size()
            );

            return ResponseEntity.ok(reviews);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "GET /api/reviews/product/{} - Product not found",
                    productId
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "GET /api/reviews/product/{} - Failed to fetch reviews",
                    productId,
                    e
            );

            throw e;
        }
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "PUT /api/reviews/{} - Updating review for userId: {} with rating {}",
                reviewId,
                userId,
                request.rating()
        );

        try {
            ReviewResponse response =
                    reviewService.updateReview(
                            userId,
                            reviewId,
                            request
                    );

            log.info(
                    "PUT /api/reviews/{} - Successfully updated for userId: {}",
                    reviewId,
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "PUT /api/reviews/{} - Failed for userId: {} - {}",
                    reviewId,
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "PUT /api/reviews/{} - Unexpected error for userId: {}",
                    reviewId,
                    userId,
                    e
            );

            throw e;
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();
        boolean isAdmin = false;
        if (userDetails.getAuthorities() != null) {
            isAdmin = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN"));
        }

        log.info(
                "DELETE /api/reviews/{} - Deleting review for userId: {} isAdmin: {}",
                reviewId,
                userId,
                isAdmin
        );

        try {
            if (isAdmin) {
                reviewService.deleteAnyReview(reviewId);
                log.info(
                        "DELETE /api/reviews/{} - Successfully admin-deleted for userId: {}",
                        reviewId,
                        userId
                );
            } else {
                reviewService.deleteReview(userId, reviewId);
                log.info(
                        "DELETE /api/reviews/{} - Successfully deleted for userId: {}",
                        reviewId,
                        userId
                );
            }

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            log.warn(
                    "DELETE /api/reviews/{} - Failed for userId: {} - {}",
                    reviewId,
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "DELETE /api/reviews/{} - Unexpected error for userId: {}",
                    reviewId,
                    userId,
                    e
            );

            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(
            Pageable pageable,
            @RequestParam(required = false) Integer minRating) {

        log.info(
                "GET /api/reviews/all - Fetching all reviews (ADMIN) with minRating: {} pageable: {}",
                minRating,
                pageable
        );

        try {
            Page<ReviewResponse> page = reviewService.getAllReviews(pageable, minRating);

            log.debug(
                    "GET /api/reviews/all - Retrieved {} reviews",
                    page.getNumberOfElements()
            );

            return ResponseEntity.ok(page);

        } catch (IllegalArgumentException e) {
            log.warn("GET /api/reviews/all - Failed - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("GET /api/reviews/all - Unexpected error", e);
            throw e;
        }
    }
}