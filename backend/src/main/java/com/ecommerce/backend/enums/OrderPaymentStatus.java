package com.ecommerce.backend.enums;

public enum OrderPaymentStatus {
    UNPAID,
    PAID,
    EXPIRED,
    REFUNDING,
    REFUNDED,

    @Deprecated PENDING,
    @Deprecated FAILED
}
