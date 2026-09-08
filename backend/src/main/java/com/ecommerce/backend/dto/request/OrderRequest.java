package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank(message = "Address ID cannot be empty")
        String addressId,
        String paymentMethod,
        BigDecimal shipping,
        BigDecimal tax
) {
    public OrderRequest(String addressId) {
        this(addressId, null, null, null);
    }
}
