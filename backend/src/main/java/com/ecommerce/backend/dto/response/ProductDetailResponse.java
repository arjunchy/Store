package com.ecommerce.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Boolean isNewArrival,
        Float rating,
        Integer reviewCount,
        String categoryId,
        List<ProductImageResponse> images,
        LocalDateTime createdAt
) {
}
