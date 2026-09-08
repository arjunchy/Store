package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductImageReorderRequest(
        @NotBlank(message = "Image ID cannot be empty")
        String imageId,

        @NotNull(message = "Display order cannot be null")
        Integer displayOrder
) {
}
