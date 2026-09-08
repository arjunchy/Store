package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WishlistRequest(
        @NotBlank(message = "Product ID cannot be empty")
        String productId
) {
}
