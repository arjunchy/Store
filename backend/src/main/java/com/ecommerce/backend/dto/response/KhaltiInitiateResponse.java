package com.ecommerce.backend.dto.response;

import java.time.OffsetDateTime;

public record KhaltiInitiateResponse(
        String pidx,
        // Our server-generated reference (Payment.transactionId) — never the order id.
        String transactionId,
        String paymentUrl,
        OffsetDateTime expiresAt,
        Integer expiresIn,
        String orderId,
        String purchaseOrderId,
        String purchaseOrderName,
        Integer amount
) {
}