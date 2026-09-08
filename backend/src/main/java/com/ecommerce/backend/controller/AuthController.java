package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.AuthRequest;
import com.ecommerce.backend.dto.response.AuthResponse;
import com.ecommerce.backend.dto.request.RefreshTokenRequest;
import com.ecommerce.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("POST /api/auth/login - Login attempt for email: {}", request.email());
        try {
            AuthResponse response = authService.login(request);
            log.info("POST /api/auth/login - Login successful for email: {} with role: {}", request.email(), response.role());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/auth/login - Login failed for email: {} - {}", request.email(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/auth/login - Unexpected error for email: {}", request.email(), e);
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/refresh - Refresh token request received");
        try {
            AuthResponse response = authService.refresh(request);
            log.info("POST /api/auth/refresh - Successfully refreshed tokens for email: {}", response.email());
            log.debug("POST /api/auth/refresh - New tokens generated for email: {}", response.email());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/auth/refresh - Refresh failed - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/auth/refresh - Unexpected error during token refresh", e);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/logout - Logout request received");
        try {
            authService.logout(request.refreshToken());
            log.info("POST /api/auth/logout - Successfully logged out");
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/auth/logout - Logout failed - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/auth/logout - Unexpected error during logout", e);
            throw e;
        }
    }
}
