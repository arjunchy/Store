package com.ecommerce.backend.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        String id,
        String productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {
}
