package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "Category name cannot be empty")
        String name,

        String parentId
) {
}
