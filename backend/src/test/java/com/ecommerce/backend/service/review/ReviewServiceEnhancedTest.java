package com.ecommerce.backend.service.review;

import com.ecommerce.backend.dto.response.ReviewResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.Review;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.mapper.ReviewMapper;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.ReviewRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceEnhancedTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private Product product;
    private User user;
    private Review review;

    @BeforeEach
    void setUp() {
        product = Product.builder().id("prod-1").name("Laptop").reviewCount(1).rating(4.0f).build();
        user = User.builder().userId("user-1").username("john").email("john@example.com").build();
        review = Review.builder().id("rev-1").product(product).user(user).rating(5).comment("Great").isVerifiedPurchase(true).createdAt(LocalDateTime.now()).build();

        lenient().when(reviewMapper.toResponse(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            return new ReviewResponse(r.getId(), r.getProduct() != null ? r.getProduct().getId() : null,
                    r.getUser() != null ? r.getUser().getUserId() : null, r.getUser() != null ? r.getUser().getUsername() : null, r.getRating(),
                    r.getComment(), r.getIsVerifiedPurchase(), r.getCreatedAt());
        });
    }

    @Test
    void getAllReviews_withoutMinRating_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);
        when(reviewRepository.findAll(pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getAllReviews(pageable, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("rev-1");
        verify(reviewRepository).findAll(pageable);
        verify(reviewRepository, never()).findByRatingGreaterThanEqual(anyInt(), any());
    }

    @Test
    void getAllReviews_withMinRating_filters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);
        when(reviewRepository.findByRatingGreaterThanEqual(4, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getAllReviews(pageable, 4);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(reviewRepository).findByRatingGreaterThanEqual(4, pageable);
    }

    @Test
    void getAllReviews_invalidMinRating_low_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> reviewService.getAllReviews(pageable, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minRating must be between");
    }

    @Test
    void getAllReviews_invalidMinRating_high_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> reviewService.getAllReviews(pageable, 6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteAnyReview_success() {
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(review));
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.deleteAnyReview("rev-1");

        verify(reviewRepository).delete(review);
        verify(productRepository).save(product);
        assertThat(product.getReviewCount()).isEqualTo(0);
        assertThat(product.getRating()).isEqualTo(0.0f);
    }

    @Test
    void deleteAnyReview_notFound_throws() {
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewService.deleteAnyReview("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void deleteAnyReview_updatesAggregates() {
        Product p = Product.builder().id("prod-1").name("Phone").reviewCount(2).rating(4.0f).build();
        Review r1 = Review.builder().id("rev-1").product(p).user(user).rating(5).build();
        Review r2 = Review.builder().id("rev-2").product(p).user(User.builder().userId("user-2").build()).rating(3).build();
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(r1));
        // After deletion, findByProductId returns remaining 1 review (r2)
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of(r2));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.deleteAnyReview("rev-1");

        verify(reviewRepository).delete(r1);
        // After deletion, product should have rating 3.0 (average of remaining)
        assertThat(p.getReviewCount()).isEqualTo(1);
        assertThat(p.getRating()).isEqualTo(3.0f);
    }
}
