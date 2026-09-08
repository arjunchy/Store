package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        return user;
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserRole(),
                user.getCreatedAt(),
                user.getDeletedAt(),
                user.getDeletedAt() != null
        );
    }
}
