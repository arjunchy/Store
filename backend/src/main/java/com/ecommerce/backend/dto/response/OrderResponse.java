package com.ecommerce.backend.dto.response;

import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String orderNumber,
        String userId,
        String userEmail,
        String userName,
        BigDecimal totalAmount,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal tax,
        String paymentMethod,
        OrderStatus status,
        OrderPaymentStatus paymentStatus,
        DeliveryStatus deliveryStatus,
        String shippingAddress,
        String trackingNumber,
        String carrier,
        java.time.LocalDate estimatedDelivery,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public OrderResponse(String id, String orderNumber, BigDecimal totalAmount, OrderStatus status, OrderPaymentStatus paymentStatus, DeliveryStatus deliveryStatus, List<OrderItemResponse> items, LocalDateTime createdAt, String userId, String userEmail, String userName) {
        this(id, orderNumber, userId, userEmail, userName, totalAmount, null, null, null, null, status, paymentStatus, deliveryStatus, null, null, null, null, items, createdAt, null);
    }

    public OrderResponse withAmounts(BigDecimal subtotal, BigDecimal shippingCost, BigDecimal tax, String paymentMethod) {
        return new OrderResponse(
                this.id, this.orderNumber, this.userId, this.userEmail, this.userName,
                this.totalAmount, subtotal, shippingCost, tax, paymentMethod,
                this.status, this.paymentStatus, this.deliveryStatus,
                this.shippingAddress, this.trackingNumber, this.carrier, this.estimatedDelivery,
                this.items, this.createdAt, this.updatedAt
        );
    }
}
