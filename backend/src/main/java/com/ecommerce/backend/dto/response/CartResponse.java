package com.ecommerce.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        String id,
        String userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
