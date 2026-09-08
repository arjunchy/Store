package com.ecommerce.backend.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {
}
