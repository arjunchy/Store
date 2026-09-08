package com.ecommerce.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Boolean isNewArrival,
        Float rating,
        Integer reviewCount,
        String categoryId,
        LocalDateTime createdAt
) {
}
