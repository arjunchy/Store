package com.ecommerce.backend.enums;

public enum OrderStatus {
    PROCESSING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELED,
    COMPLETED,

    @Deprecated PENDING,
    @Deprecated CANCELLED
}
