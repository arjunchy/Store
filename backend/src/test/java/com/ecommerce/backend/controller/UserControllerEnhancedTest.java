package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerEnhancedTest {

    @Mock private UserService userService;
    @InjectMocks private UserController userController;

    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
    }

    @Test
    void getAllUsers_withIncludeDeleted_returns200() {
        when(userService.getAllUsers(true)).thenReturn(List.of(userResponse));
        ResponseEntity<?> resp = userController.getAllUsers(true, null, null, null, false);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).getAllUsers(true);
    }

    @Test
    void getAllUsers_withoutIncludeDeleted_returns200() {
        when(userService.getAllUsers(false)).thenReturn(List.of(userResponse));
        ResponseEntity<?> resp = userController.getAllUsers(false, null, null, null, false);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).getAllUsers(false);
    }

    @Test
    void changeUserRole_success_returns200() {
        UserResponse adminResp = new UserResponse("user-1", "john", "john@example.com", UserRole.ADMIN, LocalDateTime.now());
        when(userService.changeUserRole("user-1", UserRole.ADMIN)).thenReturn(adminResp);
        ResponseEntity<UserResponse> resp = userController.changeUserRole("user-1", Map.of("role", "ADMIN"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().userRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void changeUserRole_withRolePrefix_success() {
        UserResponse adminResp = new UserResponse("user-1", "john", "john@example.com", UserRole.ADMIN, LocalDateTime.now());
        when(userService.changeUserRole("user-1", UserRole.ADMIN)).thenReturn(adminResp);
        ResponseEntity<UserResponse> resp = userController.changeUserRole("user-1", Map.of("role", "ROLE_ADMIN"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changeUserRole_newRoleKey_success() {
        UserResponse adminResp = new UserResponse("user-1", "john", "john@example.com", UserRole.ADMIN, LocalDateTime.now());
        when(userService.changeUserRole("user-1", UserRole.ADMIN)).thenReturn(adminResp);
        ResponseEntity<UserResponse> resp = userController.changeUserRole("user-1", Map.of("newRole", "ADMIN"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changeUserRole_missingRole_throws() {
        assertThatThrownBy(() -> userController.changeUserRole("user-1", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role is required");
    }

    @Test
    void changeUserRole_invalidRole_throws() {
        assertThatThrownBy(() -> userController.changeUserRole("user-1", Map.of("role", "INVALID")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void toggleUserStatus_activate_success() {
        UserResponse respUser = new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
        when(userService.toggleUserStatus("user-1", true)).thenReturn(respUser);
        ResponseEntity<UserResponse> resp = userController.toggleUserStatus("user-1", Map.of("activate", true));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).toggleUserStatus("user-1", true);
    }

    @Test
    void toggleUserStatus_withActiveKey_success() {
        UserResponse respUser = new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
        when(userService.toggleUserStatus("user-1", false)).thenReturn(respUser);
        ResponseEntity<UserResponse> resp = userController.toggleUserStatus("user-1", Map.of("active", false));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void toggleUserStatus_missingActivate_throws() {
        assertThatThrownBy(() -> userController.toggleUserStatus("user-1", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activate is required");
    }

    @Test
    void toggleUserStatus_stringBoolean_parsed() {
        UserResponse respUser = new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
        when(userService.toggleUserStatus("user-1", true)).thenReturn(respUser);
        ResponseEntity<UserResponse> resp = userController.toggleUserStatus("user-1", Map.of("activate", "true"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).toggleUserStatus("user-1", true);
    }

    @Test
    void hardDeleteUser_success_returns204() {
        doNothing().when(userService).hardDeleteUser("user-1");
        ResponseEntity<Void> resp = userController.hardDeleteUser("user-1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).hardDeleteUser("user-1");
    }

    @Test
    void hardDeleteUser_notFound_propagates() {
        doThrow(new IllegalArgumentException("User not found")).when(userService).hardDeleteUser("missing");
        assertThatThrownBy(() -> userController.hardDeleteUser("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
