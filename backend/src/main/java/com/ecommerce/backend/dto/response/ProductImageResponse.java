package com.ecommerce.backend.dto.response;

public record ProductImageResponse(
        String id,
        String productId,
        String url,
        String altText,
        Integer displayOrder,
        Boolean isPrimary
) {
}
