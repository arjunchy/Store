package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record OrderFulfillmentRequest(
        @Size(max = 100, message = "Tracking number must be ≤100") String trackingNumber,
        @Size(max = 100, message = "Carrier must be ≤100") String carrier,
        LocalDate estimatedDelivery
) {
}
