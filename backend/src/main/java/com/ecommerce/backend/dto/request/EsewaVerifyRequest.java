package com.ecommerce.backend.dto.request;

public record EsewaVerifyRequest(
        String data,
        String transactionUuid,
        String transactionCode,
        String totalAmount
) {
}
