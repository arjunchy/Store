package com.ecommerce.backend.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
        String id,
        String name,
        String parentId,
        LocalDateTime createdAt
) {
}
