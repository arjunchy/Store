package com.ecommerce.backend.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
        String id,
        String productId,
        String userId,
        String userName,
        Integer rating,
        String comment,
        Boolean isVerifiedPurchase,
        LocalDateTime createdAt
) {
}
