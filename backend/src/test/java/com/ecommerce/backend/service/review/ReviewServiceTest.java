package com.ecommerce.backend.service.review;

import com.ecommerce.backend.dto.request.ReviewRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").build();
        product = Product.builder().id("prod-1").name("Laptop").rating(0.0f).reviewCount(0).build();
    }

    @Test
    void createReview_success() {
        ReviewRequest request = new ReviewRequest("prod-1", 5, "Great!");
        Review saved = Review.builder().id("rev-1").product(product).user(user).rating(5).comment("Great!").isVerifiedPurchase(false).createdAt(LocalDateTime.now()).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(false);
        when(orderItemRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(true);
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of(saved));
        when(reviewMapper.toResponse(saved)).thenReturn(
                new ReviewResponse("rev-1", "prod-1", "user-1", "testuser", 5, "Great!", true, LocalDateTime.now())
        );

        ReviewResponse response = reviewService.createReview("user-1", request);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.isVerifiedPurchase()).isTrue();
        verify(productRepository).save(argThat(p -> p.getReviewCount() == 1));
    }

    @Test
    void createReview_notVerifiedPurchase() {
        ReviewRequest request = new ReviewRequest("prod-1", 3, "OK");
        Review saved = Review.builder().id("rev-1").product(product).user(user).rating(3).isVerifiedPurchase(false).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(false);
        when(orderItemRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(false);
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewMapper.toResponse(saved)).thenReturn(
                new ReviewResponse("rev-1", "prod-1", "user-1", "testuser", 3, "OK", false, LocalDateTime.now())
        );

        ReviewResponse response = reviewService.createReview("user-1", request);

        assertThat(response.isVerifiedPurchase()).isFalse();
    }

    @Test
    void createReview_alreadyReviewed_throws() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview("user-1", new ReviewRequest("prod-1", 5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void createReview_userNotFound_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("missing", new ReviewRequest("prod-1", 5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createReview_productNotFound_throws() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("user-1", new ReviewRequest("missing", 5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getReviewsForProduct_success() {
        Review review = Review.builder().id("rev-1").product(product).user(user).rating(5).build();
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(reviewRepository.findByProductIdWithUser("prod-1")).thenReturn(List.of(review));
        when(reviewMapper.toResponse(review)).thenReturn(
                new ReviewResponse("rev-1", "prod-1", "user-1", "testuser", 5, null, false, LocalDateTime.now())
        );

        List<ReviewResponse> result = reviewService.getReviewsForProduct("prod-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void getReviewsForProduct_productNotFound_throws() {
        when(productRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviewsForProduct("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void updateReview_success() {
        Review existing = Review.builder().id("rev-1").product(product).user(user).rating(3).comment("Old").isVerifiedPurchase(false).build();
        ReviewRequest request = new ReviewRequest("prod-1", 5, "Updated");
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(existing));
        when(orderItemRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(true);
        when(reviewRepository.save(any())).thenReturn(existing);
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of(existing));
        when(reviewMapper.toResponse(existing)).thenReturn(
                new ReviewResponse("rev-1", "prod-1", "user-1", "testuser", 5, "Updated", true, LocalDateTime.now())
        );

        ReviewResponse response = reviewService.updateReview("user-1", "rev-1", request);

        assertThat(response.rating()).isEqualTo(5);
        verify(productRepository).save(argThat(p -> p.getReviewCount() == 1));
    }

    @Test
    void updateReview_notFound_throws() {
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview("user-1", "missing", new ReviewRequest("prod-1", 5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void updateReview_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Review otherReview = Review.builder().id("rev-2").product(product).user(otherUser).build();
        when(reviewRepository.findById("rev-2")).thenReturn(Optional.of(otherReview));

        assertThatThrownBy(() -> reviewService.updateReview("user-1", "rev-2", new ReviewRequest("prod-1", 5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void updateReview_productMismatch_throws() {
        Review existing = Review.builder().id("rev-1").product(product).user(user).build();
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> reviewService.updateReview("user-1", "rev-1", new ReviewRequest("prod-2", 5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product mismatch");
    }

    @Test
    void deleteReview_success() {
        Review existing = Review.builder().id("rev-1").product(product).user(user).build();
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(existing));

        reviewService.deleteReview("user-1", "rev-1");

        verify(reviewRepository).delete(existing);
        verify(productRepository).save(argThat(p -> p.getReviewCount() == 0 && p.getRating() == 0.0f));
    }

    @Test
    void deleteReview_notFound_throws() {
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview("user-1", "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void deleteReview_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Review otherReview = Review.builder().id("rev-2").product(product).user(otherUser).build();
        when(reviewRepository.findById("rev-2")).thenReturn(Optional.of(otherReview));

        assertThatThrownBy(() -> reviewService.deleteReview("user-1", "rev-2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createReview_updatesRatingAggregates() {
        ReviewRequest request = new ReviewRequest("prod-1", 4, "Good");
        Review saved = Review.builder().id("rev-1").product(product).user(user).rating(4).build();
        Review existingReview = Review.builder().id("rev-1").product(product).user(user).rating(4).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(false);
        when(orderItemRepository.existsByProductIdAndUserId("prod-1", "user-1")).thenReturn(false);
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of(existingReview));
        when(reviewMapper.toResponse(saved)).thenReturn(
                new ReviewResponse("rev-1", "prod-1", "user-1", "testuser", 4, "Good", false, LocalDateTime.now())
        );

        reviewService.createReview("user-1", request);

        verify(productRepository).save(argThat(p -> p.getReviewCount() == 1 && p.getRating() == 4.0f));
    }
}
