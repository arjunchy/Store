package com.ecommerce.backend.dto.response;

import com.ecommerce.backend.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String username,
        String email,
        UserRole userRole,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        boolean isDeleted
) {
    public UserResponse(String id, String username, String email, UserRole userRole, LocalDateTime createdAt) {
        this(id, username, email, userRole, createdAt, null, false);
    }
}
