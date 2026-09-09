package com.ecommerce.backend.dto.response;

public record EsewaInitiateResponse(
        String gatewayUrl,
        String productCode,
        String transactionUuid,
        String totalAmount,
        String amount,
        String taxAmount,
        String productServiceCharge,
        String productDeliveryCharge,
        String successUrl,
        String failureUrl,
        String signedFieldNames,
        String signature,
        String orderId,
        String orderNumber
) {
}
