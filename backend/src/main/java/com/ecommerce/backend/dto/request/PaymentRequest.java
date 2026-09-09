package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "Order ID cannot be empty")
        String orderId,

        @NotBlank(message = "Payment method cannot be empty")
        String method,
        BigDecimal amount
) {
}
