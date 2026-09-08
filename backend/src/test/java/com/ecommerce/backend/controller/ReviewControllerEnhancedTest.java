package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.response.ReviewResponse;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.service.review.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerEnhancedTest {

    @Mock private ReviewService reviewService;
    @InjectMocks private ReviewController reviewController;

    private ReviewResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new ReviewResponse("rev-1", "prod-1", "user-1", "testuser", 5, "Great", true, LocalDateTime.now());
    }

    @Test
    void getAllReviews_success_returns200() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(reviewService.getAllReviews(pageable, null)).thenReturn(page);

        ResponseEntity<Page<ReviewResponse>> resp = reviewController.getAllReviews(pageable, null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
        verify(reviewService).getAllReviews(pageable, null);
    }

    @Test
    void getAllReviews_withMinRating_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(reviewService.getAllReviews(pageable, 4)).thenReturn(page);

        ResponseEntity<Page<ReviewResponse>> resp = reviewController.getAllReviews(pageable, 4);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reviewService).getAllReviews(pageable, 4);
    }

    @Test
    void getAllReviews_invalidMinRating_propagates() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewService.getAllReviews(pageable, 0)).thenThrow(new IllegalArgumentException("minRating must be between"));

        assertThatThrownBy(() -> reviewController.getAllReviews(pageable, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteReview_asAdmin_callsDeleteAny() {
        User adminUser = User.builder().userId("admin-1").username("admin").email("admin@example.com").passwordHash("hash").userRole(UserRole.ADMIN).build();
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        // Ensure authorities contain ROLE_ADMIN
        doNothing().when(reviewService).deleteAnyReview("rev-1");

        ResponseEntity<Void> resp = reviewController.deleteReview("rev-1", adminDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reviewService).deleteAnyReview("rev-1");
        verify(reviewService, never()).deleteReview(anyString(), anyString());
    }

    @Test
    void deleteReview_asUser_callsDeleteReview() {
        User normalUser = User.builder().userId("user-1").username("john").email("john@example.com").passwordHash("hash").userRole(UserRole.USER).build();
        CustomUserDetails userDetails = new CustomUserDetails(normalUser);
        doNothing().when(reviewService).deleteReview("user-1", "rev-1");

        ResponseEntity<Void> resp = reviewController.deleteReview("rev-1", userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reviewService).deleteReview("user-1", "rev-1");
        verify(reviewService, never()).deleteAnyReview(anyString());
    }

    @Test
    void deleteReview_asAdmin_notFound_propagates() {
        User adminUser = User.builder().userId("admin-1").username("admin").email("admin@example.com").passwordHash("hash").userRole(UserRole.ADMIN).build();
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        doThrow(new IllegalArgumentException("Review not found")).when(reviewService).deleteAnyReview("missing");

        assertThatThrownBy(() -> reviewController.deleteReview("missing", adminDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteReview_asUser_withMockAuthoritiesNull_handled() {
        // Mock userDetails that returns null authorities (like old test)
        CustomUserDetails mockDetails = mock(CustomUserDetails.class);
        when(mockDetails.getUserId()).thenReturn("user-1");
        when(mockDetails.getAuthorities()).thenReturn(null);
        doNothing().when(reviewService).deleteReview("user-1", "rev-1");

        ResponseEntity<Void> resp = reviewController.deleteReview("rev-1", mockDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reviewService).deleteReview("user-1", "rev-1");
    }
}
