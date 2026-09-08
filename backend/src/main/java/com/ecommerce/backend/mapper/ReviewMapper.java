package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.ReviewResponse;
import com.ecommerce.backend.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        return new ReviewResponse(
                review.getId(),
                review.getProduct() != null ? review.getProduct().getId() : null,
                review.getUser() != null ? review.getUser().getUserId() : null,
                review.getUser() != null ? review.getUser().getUsername() : null,
                review.getRating(),
                review.getComment(),
                review.getIsVerifiedPurchase(),
                review.getCreatedAt()
        );
    }
}
