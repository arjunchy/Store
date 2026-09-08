package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.AddressResponse;
import com.ecommerce.backend.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getIsDefault(),
                address.getCreatedAt()
        );
    }
}
