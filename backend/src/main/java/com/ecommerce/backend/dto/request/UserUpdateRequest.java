package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Please provide a valid email address")
        String email,

        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {
}
