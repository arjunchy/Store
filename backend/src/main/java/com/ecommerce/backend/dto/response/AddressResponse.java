package com.ecommerce.backend.dto.response;

import java.time.LocalDateTime;

public record AddressResponse(
        String id,
        String label,
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        Boolean isDefault,
        LocalDateTime createdAt
) {
}
