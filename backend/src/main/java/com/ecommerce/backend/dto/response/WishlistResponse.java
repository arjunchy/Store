package com.ecommerce.backend.dto.response;

import java.time.LocalDateTime;

public record WishlistResponse(
        String id,
        String productId,
        String productName,
        LocalDateTime createdAt
) {
}
