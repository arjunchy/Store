package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toEntity_mapsUsernameAndEmail() {
        UserRequest request = new UserRequest("john", "john@example.com", "password123");

        User user = mapper.toEntity(request);

        assertThat(user.getUsername()).isEqualTo("john");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        // should not map passwordHash or role here; service handles those
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getUserId()).isNull();
    }

    @Test
    void toResponse_mapsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .userId("uuid-123")
                .username("john")
                .email("john@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.ADMIN)
                .createdAt(now)
                .build();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo("uuid-123");
        assertThat(response.username()).isEqualTo("john");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.userRole()).isEqualTo(UserRole.ADMIN);
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_doesNotExposePasswordHash() {
        User user = User.builder()
                .userId("1")
                .username("john")
                .email("john@example.com")
                .passwordHash("super-secret-hash")
                .userRole(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();

        UserResponse response = mapper.toResponse(user);

        // UserResponse has no passwordHash field - ensure mapper does not leak it via toString or otherwise
        assertThat(response.toString()).doesNotContain("super-secret-hash");
    }

    @Test
    void toEntity_handlesNullFieldsGracefully() {
        UserRequest request = new UserRequest(null, null, null);

        User user = mapper.toEntity(request);

        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isNull();
    }
}
