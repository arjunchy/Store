package com.ecommerce.backend.service.review;

import com.ecommerce.backend.dto.request.ReviewRequest;
import com.ecommerce.backend.dto.response.ReviewResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.Review;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.ReviewRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.mapper.ReviewMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(String userId, ReviewRequest request) {
        log.info("Creating review for productId: {} by userId: {} with rating: {}", request.productId(), userId, request.rating());
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found with id: {}", userId);
                        return new IllegalArgumentException("User not found");
                    });

            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> {
                        log.warn("Product not found with id: {}", request.productId());
                        return new IllegalArgumentException("Product not found");
                    });

            if (reviewRepository.existsByProductIdAndUserId(request.productId(), userId)) {
                log.warn("User {} already reviewed product {}", userId, request.productId());
                throw new IllegalArgumentException("You have already reviewed this product");
            }

            boolean isVerifiedPurchase = false;
            try {
                isVerifiedPurchase = orderItemRepository.existsVerifiedPurchase(request.productId(), userId);
                log.debug("isVerifiedPurchase for userId: {} productId: {} = {}", userId, request.productId(), isVerifiedPurchase);
            } catch (Exception e) {
                log.error("Failed to check verified purchase for userId: {} productId: {}", userId, request.productId(), e);
                throw e;
            }

            Review review = Review.builder()
                    .product(product)
                    .user(user)
                    .rating(request.rating())
                    .comment(request.comment())
                    .isVerifiedPurchase(isVerifiedPurchase)
                    .build();

            Review saved = reviewRepository.save(review);
            log.info("Successfully created review with id: {} for product {} by user {}", saved.getId(), request.productId(), userId);

            updateProductAggregates(product);

            return reviewMapper.toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create review for productId: {} by userId: {}", request.productId(), userId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForProduct(String productId) {
        log.debug("Fetching reviews for productId: {}", productId);
        try {
            if (!productRepository.existsById(productId)) {
                log.warn("Product not found with id: {}", productId);
                throw new IllegalArgumentException("Product not found");
            }
            List<Review> reviews = reviewRepository.findByProductIdWithUser(productId);
            log.info("Retrieved {} reviews for productId: {}", reviews.size(), productId);
            return reviews.stream()
                    .map(reviewMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch reviews for productId: {}", productId, e);
            throw e;
        }
    }

    @Transactional
    public ReviewResponse updateReview(String userId, String reviewId, ReviewRequest request) {
        log.info("Updating review {} for userId: {}", reviewId, userId);
        try {
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> {
                        log.warn("Review not found with id: {}", reviewId);
                        return new IllegalArgumentException("Review not found");
                    });

            if (!review.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - review {} does not belong to user {}", reviewId, userId);
                throw new IllegalArgumentException("Review not found");
            }

            if (!review.getProduct().getId().equals(request.productId())) {
                log.warn("Product mismatch for review update: review product {} vs request product {}", review.getProduct().getId(), request.productId());
                throw new IllegalArgumentException("Product mismatch");
            }

            review.setRating(request.rating());
            review.setComment(request.comment());

            try {
                boolean isVerified = orderItemRepository.existsVerifiedPurchase(request.productId(), userId);
                review.setIsVerifiedPurchase(isVerified);
            } catch (Exception e) {
                log.error("Failed to re-evaluate verified purchase for review {}", reviewId, e);
                throw e;
            }

            Review saved = reviewRepository.save(review);
            log.info("Successfully updated review {} for userId: {}", reviewId, userId);

            updateProductAggregates(review.getProduct());

            return reviewMapper.toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update review {} for userId: {}", reviewId, userId, e);
            throw e;
        }
    }

    @Transactional
    public void deleteReview(String userId, String reviewId) {
        log.info("Deleting review {} for userId: {}", reviewId, userId);
        try {
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> {
                        log.warn("Review not found with id: {}", reviewId);
                        return new IllegalArgumentException("Review not found");
                    });

            if (!review.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - review {} does not belong to user {}", reviewId, userId);
                throw new IllegalArgumentException("Review not found");
            }

            reviewRepository.delete(review);
            updateProductAggregates(review.getProduct());
            log.info("Successfully deleted review {} for userId: {}", reviewId, userId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete review {} for userId: {}", reviewId, userId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(Pageable pageable, Integer minRating) {
        log.debug("Fetching all reviews with pageable: {} minRating: {}", pageable, minRating);
        try {
            Page<Review> page;
            if (minRating != null) {
                if (minRating < 1 || minRating > 5) {
                    log.warn("Invalid minRating: {}", minRating);
                    throw new IllegalArgumentException("minRating must be between 1 and 5");
                }
                page = reviewRepository.findByRatingGreaterThanEqual(minRating, pageable);
                log.info("Retrieved {} reviews with minRating {} (total {} across {} pages)", page.getNumberOfElements(), minRating, page.getTotalElements(), page.getTotalPages());
            } else {
                page = reviewRepository.findAll(pageable);
                log.info("Retrieved {} reviews (total {} across {} pages)", page.getNumberOfElements(), page.getTotalElements(), page.getTotalPages());
            }
            return page.map(reviewMapper::toResponse);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch all reviews", e);
            throw e;
        }
    }

    @Transactional
    public void deleteAnyReview(String reviewId) {
        log.info("Admin deleting review with id: {}", reviewId);
        try {
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> {
                        log.warn("Review not found with id: {}", reviewId);
                        return new IllegalArgumentException("Review not found");
                    });
            reviewRepository.delete(review);
            updateProductAggregates(review.getProduct());
            log.info("Successfully admin-deleted review with id: {}", reviewId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to admin-delete review with id: {}", reviewId, e);
            throw e;
        }
    }

    private void updateProductAggregates(Product product) {
        List<Review> reviews = reviewRepository.findByProductId(product.getId());
        int count = reviews.size();
        product.setReviewCount(count);

        if (count == 0) {
            product.setRating(0.0f);
        } else {
            double avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            product.setRating(Math.round(avg * 10.0f) / 10.0f);
        }

        productRepository.save(product);
        log.debug("Updated product {} aggregates: rating={}, reviewCount={}", product.getId(), product.getRating(), product.getReviewCount());
    }
}
