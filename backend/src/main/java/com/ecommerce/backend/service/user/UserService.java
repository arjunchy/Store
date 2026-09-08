package com.ecommerce.backend.service.user;

import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.dto.request.UserUpdateRequest;
import com.ecommerce.backend.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse register(UserRequest request);

    UserResponse getCurrentUser(String email);

    List<UserResponse> getAll();

    UserResponse updateCurrentUser(String email, UserUpdateRequest request);

    void deleteCurrentUser(String email);

    List<UserResponse> getAllUsers(boolean includeDeleted);

    Page<UserResponse> getAllUsersPaginated(boolean includeDeleted, String search, Pageable pageable);

    UserResponse changeUserRole(String userId, UserRole newRole);

    UserResponse toggleUserStatus(String userId, boolean activate);

    void hardDeleteUser(String userId);
}