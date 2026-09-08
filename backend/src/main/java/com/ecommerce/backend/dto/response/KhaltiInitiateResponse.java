package com.ecommerce.backend.dto.response;

import java.time.OffsetDateTime;

public record KhaltiInitiateResponse(
        String pidx,
        String paymentUrl,
        OffsetDateTime expiresAt,
        Integer expiresIn,
        String orderId,
        String purchaseOrderId,
        String purchaseOrderName,
        Integer amount
) {
}