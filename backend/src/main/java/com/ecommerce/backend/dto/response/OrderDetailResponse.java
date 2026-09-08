package com.ecommerce.backend.dto.response;

import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        String id,
        String orderNumber,
        String userId,
        String userEmail,
        String userName,
        BigDecimal totalAmount,
        OrderStatus status,
        OrderPaymentStatus paymentStatus,
        DeliveryStatus deliveryStatus,
        String shippingAddress,
        String trackingNumber,
        String carrier,
        LocalDate estimatedDelivery,
        List<OrderItemResponse> items,
        List<OrderStatusHistoryResponse> statusHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
