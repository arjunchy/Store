package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank(message = "Address ID cannot be empty")
        String addressId,
        String paymentMethod,
        BigDecimal shipping,
        BigDecimal tax,
        String shippingMethod,
        String deliveryMethod
) {
    public OrderRequest(String addressId) {
        this(addressId, null, null, null, null, null);
    }

    public OrderRequest(String addressId, String paymentMethod, BigDecimal shipping, BigDecimal tax) {
        this(addressId, paymentMethod, shipping, tax, null, null);
    }
}
