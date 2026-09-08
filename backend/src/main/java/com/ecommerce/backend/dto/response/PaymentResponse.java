package com.ecommerce.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String orderId,
        BigDecimal amount,
        String method,
        String transactionId,
        String status,
        LocalDateTime createdAt
) {
}
