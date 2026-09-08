package com.ecommerce.backend.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role
) {
}
