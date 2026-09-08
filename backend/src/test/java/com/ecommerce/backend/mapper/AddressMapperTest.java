package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.AddressResponse;
import com.ecommerce.backend.entity.Address;
import com.ecommerce.backend.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AddressMapperTest {

    private final AddressMapper mapper = new AddressMapper();

    @Test
    void toResponse_mapsAllFields() {
        User user = User.builder().userId("user-1").build();
        LocalDateTime now = LocalDateTime.now();
        Address address = Address.builder()
                .id("addr-1")
                .user(user)
                .label("Home")
                .street("123 Main St")
                .city("Springfield")
                .state("IL")
                .postalCode("62701")
                .country("US")
                .isDefault(true)
                .createdAt(now)
                .build();

        AddressResponse response = mapper.toResponse(address);

        assertThat(response.id()).isEqualTo("addr-1");
        assertThat(response.label()).isEqualTo("Home");
        assertThat(response.street()).isEqualTo("123 Main St");
        assertThat(response.city()).isEqualTo("Springfield");
        assertThat(response.state()).isEqualTo("IL");
        assertThat(response.postalCode()).isEqualTo("62701");
        assertThat(response.country()).isEqualTo("US");
        assertThat(response.isDefault()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_handlesNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_handlesNullLabelAndState() {
        User user = User.builder().userId("user-1").build();
        Address address = Address.builder()
                .id("addr-2")
                .user(user)
                .label(null)
                .street("456 Oak Ave")
                .city("Chicago")
                .state(null)
                .postalCode("60601")
                .country("US")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .build();

        AddressResponse response = mapper.toResponse(address);

        assertThat(response.label()).isNull();
        assertThat(response.state()).isNull();
        assertThat(response.isDefault()).isFalse();
    }

    @Test
    void toResponse_isDefaultFalse() {
        User user = User.builder().userId("user-1").build();
        Address address = Address.builder()
                .id("addr-3")
                .user(user)
                .label("Work")
                .street("789 Pine Rd")
                .city("Naperville")
                .state("IL")
                .postalCode("60540")
                .country("US")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .build();

        AddressResponse response = mapper.toResponse(address);

        assertThat(response.isDefault()).isFalse();
    }
}
