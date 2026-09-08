package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KhaltiLookupRequest(
        @NotBlank String pidx
) {
}