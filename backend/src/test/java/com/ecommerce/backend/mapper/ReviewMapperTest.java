package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.ReviewResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.Review;
import com.ecommerce.backend.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewMapperTest {

    private final ReviewMapper mapper = new ReviewMapper();

    @Test
    void toResponse_mapsAllFields() {
        Product product = Product.builder().id("prod-1").name("Laptop").build();
        User user = User.builder().userId("user-1").build();
        LocalDateTime now = LocalDateTime.now();
        Review review = Review.builder()
                .id("rev-1")
                .product(product)
                .user(user)
                .rating(5)
                .comment("Great product!")
                .isVerifiedPurchase(true)
                .createdAt(now)
                .build();

        ReviewResponse response = mapper.toResponse(review);

        assertThat(response.id()).isEqualTo("rev-1");
        assertThat(response.productId()).isEqualTo("prod-1");
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Great product!");
        assertThat(response.isVerifiedPurchase()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_handlesNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_handlesNullProductAndUser() {
        Review review = Review.builder()
                .id("rev-2")
                .product(null)
                .user(null)
                .rating(3)
                .comment(null)
                .isVerifiedPurchase(false)
                .createdAt(LocalDateTime.now())
                .build();

        ReviewResponse response = mapper.toResponse(review);

        assertThat(response.productId()).isNull();
        assertThat(response.userId()).isNull();
        assertThat(response.comment()).isNull();
        assertThat(response.isVerifiedPurchase()).isFalse();
    }
}
