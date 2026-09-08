package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.AuthRequest;
import com.ecommerce.backend.dto.response.AuthResponse;
import com.ecommerce.backend.dto.request.RefreshTokenRequest;
import com.ecommerce.backend.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_success_returns200() {
        AuthRequest req = new AuthRequest("john@example.com", "password123");
        AuthResponse resp = new AuthResponse("access", "refresh", "john@example.com", "ROLE_USER");
        when(authService.login(any(AuthRequest.class))).thenReturn(resp);

        ResponseEntity<AuthResponse> response = authController.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isEqualTo("access");
        assertThat(response.getBody().email()).isEqualTo("john@example.com");
        verify(authService).login(req);
    }

    @Test
    void login_delegatesToService() {
        AuthRequest req = new AuthRequest("a@b.com", "pass");
        AuthResponse resp = new AuthResponse("a", "r", "a@b.com", "ROLE_USER");
        when(authService.login(any())).thenReturn(resp);

        authController.login(req);

        verify(authService).login(req);
    }

    @Test
    void login_propagatesIllegalArgument() {
        AuthRequest req = new AuthRequest("john@example.com", "wrong");
        when(authService.login(any())).thenThrow(new IllegalArgumentException("Invalid credentials"));

        assertThatThrownBy(() -> authController.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void refresh_success_returns200() {
        RefreshTokenRequest req = new RefreshTokenRequest("valid-refresh-token");
        AuthResponse resp = new AuthResponse("new-access", "new-refresh", "john@example.com", "ROLE_USER");
        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(resp);

        ResponseEntity<AuthResponse> response = authController.refresh(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isEqualTo("new-access");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh");
        verify(authService).refresh(req);
    }

    @Test
    void refresh_delegatesToService() {
        RefreshTokenRequest req = new RefreshTokenRequest("some-token");
        when(authService.refresh(any())).thenReturn(new AuthResponse("a", "r", "e", "ROLE_USER"));

        authController.refresh(req);

        verify(authService).refresh(req);
    }

    @Test
    void refresh_propagatesInvalidTokenException() {
        RefreshTokenRequest req = new RefreshTokenRequest("bad-token");
        when(authService.refresh(any())).thenThrow(new IllegalArgumentException("Invalid refresh token"));

        assertThatThrownBy(() -> authController.refresh(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_propagatesExpiredException() {
        RefreshTokenRequest req = new RefreshTokenRequest("expired");
        when(authService.refresh(any())).thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

        assertThatThrownBy(() -> authController.refresh(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");
    }
}
