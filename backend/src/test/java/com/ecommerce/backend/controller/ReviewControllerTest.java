package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.ReviewRequest;
import com.ecommerce.backend.dto.response.ReviewResponse;
import com.ecommerce.backend.service.review.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private CustomUserDetails userDetails;

    private ReviewResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new ReviewResponse(
                "rev-1", "prod-1", "user-1", "testuser", 5,
                "Great product!", true, LocalDateTime.now()
        );
    }

    @Test
    void createReview_success_returns201() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(reviewService.createReview(eq("user-1"), any(ReviewRequest.class))).thenReturn(sampleResponse);

        ReviewRequest request = new ReviewRequest("prod-1", 5, "Great product!");
        ResponseEntity<ReviewResponse> response = reviewController.createReview(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo("rev-1");
        assertThat(response.getBody().rating()).isEqualTo(5);
        verify(reviewService).createReview("user-1", request);
    }

    @Test
    void createReview_alreadyReviewed_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(reviewService.createReview(eq("user-1"), any())).thenThrow(new IllegalArgumentException("You have already reviewed this product"));

        assertThatThrownBy(() -> reviewController.createReview(new ReviewRequest("prod-1", 4, "Nice"), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void createReview_productNotFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(reviewService.createReview(eq("user-1"), any())).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> reviewController.createReview(new ReviewRequest("missing", 5, null), userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getReviewsForProduct_success_returns200() {
        when(reviewService.getReviewsForProduct("prod-1")).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<ReviewResponse>> response = reviewController.getReviewsForProduct("prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).rating()).isEqualTo(5);
    }

    @Test
    void getReviewsForProduct_empty_returnsEmptyList() {
        when(reviewService.getReviewsForProduct("prod-1")).thenReturn(List.of());

        ResponseEntity<List<ReviewResponse>> response = reviewController.getReviewsForProduct("prod-1");

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getReviewsForProduct_productNotFound_propagates() {
        when(reviewService.getReviewsForProduct("missing")).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> reviewController.getReviewsForProduct("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateReview_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        ReviewResponse updated = new ReviewResponse("rev-1", "prod-1", "user-1", "testuser", 4, "Updated comment", true, LocalDateTime.now());
        when(reviewService.updateReview(eq("user-1"), eq("rev-1"), any(ReviewRequest.class))).thenReturn(updated);

        ReviewRequest request = new ReviewRequest("prod-1", 4, "Updated comment");
        ResponseEntity<ReviewResponse> response = reviewController.updateReview("rev-1", request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().rating()).isEqualTo(4);
    }

    @Test
    void updateReview_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(reviewService.updateReview(eq("user-1"), eq("missing"), any())).thenThrow(new IllegalArgumentException("Review not found"));

        assertThatThrownBy(() -> reviewController.updateReview("missing", new ReviewRequest("prod-1", 3, null), userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateReview_notOwner_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(reviewService.updateReview(eq("user-1"), eq("rev-1"), any())).thenThrow(new IllegalArgumentException("Review not found"));

        assertThatThrownBy(() -> reviewController.updateReview("rev-1", new ReviewRequest("prod-1", 3, null), userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteReview_success_returns204() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doNothing().when(reviewService).deleteReview("user-1", "rev-1");

        ResponseEntity<Void> response = reviewController.deleteReview("rev-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reviewService).deleteReview("user-1", "rev-1");
    }

    @Test
    void deleteReview_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doThrow(new IllegalArgumentException("Review not found")).when(reviewService).deleteReview("user-1", "missing");

        assertThatThrownBy(() -> reviewController.deleteReview("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
