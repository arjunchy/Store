package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name cannot be empty")
        String name,

        @NotBlank(message = "Product description cannot be empty")
        String description,

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be >= 0")
        BigDecimal price,

        @NotNull(message = "Stock quantity cannot be null")
        @Min(value = 0, message = "Stock quantity must be >= 0")
        Integer stockQuantity,

        Boolean isNewArrival,

        String categoryId
) {
}
