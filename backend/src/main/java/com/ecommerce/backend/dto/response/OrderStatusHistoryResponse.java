package com.ecommerce.backend.dto.response;

import com.ecommerce.backend.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(
        String id,
        String orderId,
        OrderStatus status,
        String statusType,
        String note,
        String changedBy,
        LocalDateTime createdAt
) {
}
