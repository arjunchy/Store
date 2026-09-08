package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String label,

        @NotBlank(message = "Street cannot be empty")
        String street,

        @NotBlank(message = "City cannot be empty")
        String city,

        String state,

        @NotBlank(message = "Postal code cannot be empty")
        String postalCode,

        @NotBlank(message = "Country cannot be empty")
        String country,

        Boolean isDefault
) {

    public AddressRequest {
        if (isDefault == null) {
            isDefault = false;
        }
    }
}
