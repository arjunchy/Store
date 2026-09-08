package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.dto.request.UserUpdateRequest;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private CustomUserDetails userDetails;
    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .userId("uuid-1")
                .username("john")
                .email("john@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.USER)
                .build();
        userDetails = new CustomUserDetails(user);
        sampleResponse = new UserResponse("uuid-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
    }

    @Test
    void register_success_returns201() {
        UserRequest req = new UserRequest("john", "john@example.com", "password123");
        when(userService.register(any(UserRequest.class))).thenReturn(sampleResponse);

        var response = userController.register(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(sampleResponse);
        verify(userService).register(req);
    }

    @Test
    void getCurrentUser_success() {
        when(userService.getCurrentUser("john@example.com")).thenReturn(sampleResponse);

        var response = userController.getCurrentUser(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo("john@example.com");
        verify(userService).getCurrentUser("john@example.com");
    }

    @Test
    void getCurrentUser_delegatesToServiceWithEmail() {
        UserResponse adminResp = new UserResponse("uuid-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
        when(userService.getCurrentUser("john@example.com")).thenReturn(adminResp);

        var response = userController.getCurrentUser(userDetails);

        assertThat(response.getBody()).isEqualTo(adminResp);
    }

    @Test
    void getAll_returnsList() {
        UserResponse r2 = new UserResponse("2", "jane", "jane@example.com", UserRole.USER, LocalDateTime.now());
        when(userService.getAll()).thenReturn(List.of(sampleResponse, r2));

        var response = userController.getAll();

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(userService).getAll();
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        when(userService.getAll()).thenReturn(List.of());

        var response = userController.getAll();

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void updateCurrentUser_success() {
        UserUpdateRequest req = new UserUpdateRequest("john2", "john@example.com", null);
        UserResponse updated = new UserResponse("uuid-1", "john2", "john@example.com", UserRole.USER, LocalDateTime.now());
        when(userService.updateCurrentUser(eq("john@example.com"), any(UserUpdateRequest.class))).thenReturn(updated);

        var response = userController.updateCurrentUser(userDetails, req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody().username()).isEqualTo("john2");
        verify(userService).updateCurrentUser("john@example.com", req);
    }

    @Test
    void updateCurrentUser_withNewEmailAndPassword() {
        UserUpdateRequest req = new UserUpdateRequest("john2", "new@example.com", "newPass123");
        UserResponse updated = new UserResponse("uuid-1", "john2", "new@example.com", UserRole.USER, LocalDateTime.now());
        when(userService.updateCurrentUser(eq("john@example.com"), any(UserUpdateRequest.class))).thenReturn(updated);

        var response = userController.updateCurrentUser(userDetails, req);

        assertThat(response.getBody().email()).isEqualTo("new@example.com");
    }

    @Test
    void updateCurrentUser_propagatesServiceException() {
        UserUpdateRequest req = new UserUpdateRequest("john2", "taken@example.com", null);
        when(userService.updateCurrentUser(anyString(), any(UserUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        try {
            userController.updateCurrentUser(userDetails, req);
            assertThat(false).as("should have thrown").isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Email already exists");
        }
    }

    @Test
    void deleteCurrentUser_success_returns204() {
        doNothing().when(userService).deleteCurrentUser("john@example.com");

        var response = userController.deleteCurrentUser(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(userService).deleteCurrentUser("john@example.com");
    }

    @Test
    void deleteCurrentUser_delegatesCorrectEmail() {
        userController.deleteCurrentUser(userDetails);

        verify(userService).deleteCurrentUser("john@example.com");
    }

    @Test
    void register_propagatesDuplicateEmailException() {
        UserRequest req = new UserRequest("john", "john@example.com", "password123");
        when(userService.register(any(UserRequest.class))).thenThrow(new IllegalArgumentException("Email already exists"));

        try {
            userController.register(req);
            assertThat(false).as("should have thrown").isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Email already exists");
        }
    }
}
