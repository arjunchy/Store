package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(
        @NotBlank(message = "Image URL cannot be empty")
        String url,

        String altText,

        Integer displayOrder,

        Boolean isPrimary
) {
}
