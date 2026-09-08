package com.ecommerce.backend.enums;

public enum PaymentStatus {
    UNPAID,
    PAID,
    EXPIRED,
    REFUNDING,
    REFUNDED,
    INITIATED,

    @Deprecated PENDING,
    @Deprecated COMPLETED,
    @Deprecated FAILED
}
