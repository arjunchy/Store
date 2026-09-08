package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.dto.request.UserUpdateRequest;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRequest request) {

        log.info(
                "POST /api/users - Registration request for email: {}",
                request.email()
        );

        try {
            UserResponse response = userService.register(request);

            log.info(
                    "POST /api/users - Successfully registered user with email: {} and id: {}",
                    response.email(),
                    response.id()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "POST /api/users - Registration failed for email: {} - {}",
                    request.email(),
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "POST /api/users - Unexpected error during registration for email: {}",
                    request.email(),
                    e
            );

            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getUsername();

        log.info(
                "GET /api/users/me - Fetching current user for email: {}",
                email
        );

        try {
            UserResponse user =
                    userService.getCurrentUser(email);

            log.debug(
                    "GET /api/users/me - Found user with id: {}",
                    user.id()
            );

            return ResponseEntity.ok(user);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "GET /api/users/me - User not found for email: {}",
                    email
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "GET /api/users/me - Unexpected error for email: {}",
                    email,
                    e
            );

            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {

        log.info("GET /api/users - Fetching all users (ADMIN request)");

        try {
            List<UserResponse> users = userService.getAll();

            log.info(
                    "GET /api/users - Retrieved {} users",
                    users.size()
            );

            return ResponseEntity.ok(users);

        } catch (Exception e) {

            log.error(
                    "GET /api/users - Failed to fetch all users",
                    e
            );

            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") boolean includeDeleted,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "paged", required = false, defaultValue = "false") boolean paged) {

        log.info("GET /api/users/all - Fetching all users includeDeleted={} page={} size={} search={} paged={}", includeDeleted, page, size, search, paged);

        try {
            if (paged || page != null || size != null || search != null) {
                int p = page != null ? page : 0;
                int s = size != null ? size : 20;
                if (s > 100) s = 100;
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(p, s, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at"));
                org.springframework.data.domain.Page<UserResponse> pagedResult = userService.getAllUsersPaginated(includeDeleted, search, pageable);
                return ResponseEntity.ok(pagedResult);
            }
            List<UserResponse> users = userService.getAllUsers(includeDeleted);

            log.info("GET /api/users/all - Retrieved {} users", users.size());

            return ResponseEntity.ok(users);

        } catch (Exception e) {

            log.error("GET /api/users/all - Failed to fetch all users", e);

            throw e;
        }
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {

        String email = userDetails.getUsername();

        log.info(
                "PUT /api/users/me - Update request for email: {}",
                email
        );

        try {
            UserResponse updatedUser =
                    userService.updateCurrentUser(
                            email,
                            request
                    );

            log.info(
                    "PUT /api/users/me - Successfully updated email: {}",
                    email
            );

            return ResponseEntity.ok(updatedUser);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "PUT /api/users/me - Update failed for email: {} - {}",
                    email,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "PUT /api/users/me - Unexpected error for email: {}",
                    email,
                    e
            );

            throw e;
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getUsername();

        log.info(
                "DELETE /api/users/me - Delete request for email: {}",
                email
        );

        try {
            userService.deleteCurrentUser(email);

            log.info(
                    "DELETE /api/users/me - Successfully deleted email: {}",
                    email
            );

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            log.warn(
                    "DELETE /api/users/me - Delete failed for email: {} - {}",
                    email,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "DELETE /api/users/me - Unexpected error for email: {}",
                    email,
                    e
            );

            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        log.info("PUT /api/users/{}/role - Change role request with body: {}", id, body);

        try {
            String roleStr = body.get("role");
            if (roleStr == null) {
                roleStr = body.get("newRole");
            }
            if (roleStr == null || roleStr.isBlank()) {
                log.warn("PUT /api/users/{}/role - Role is required", id);
                throw new IllegalArgumentException("Role is required");
            }

            UserRole newRole;
            try {
                String normalized = roleStr.toUpperCase().replace("ROLE_", "");
                newRole = UserRole.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                log.warn("PUT /api/users/{}/role - Invalid role: {}", id, roleStr);
                throw new IllegalArgumentException("Invalid role: " + roleStr);
            }

            UserResponse updated = userService.changeUserRole(id, newRole);

            log.info("PUT /api/users/{}/role - Successfully changed to {}", id, newRole);

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/users/{}/role - Failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PUT /api/users/{}/role - Unexpected error", id, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {

        log.info("PUT /api/users/{}/status - Toggle status request with body: {}", id, body);

        try {
            Object activateObj = body.get("activate");
            if (activateObj == null) {
                activateObj = body.get("active");
            }
            if (activateObj == null) {
                log.warn("PUT /api/users/{}/status - activate is required", id);
                throw new IllegalArgumentException("activate is required");
            }

            boolean activate;
            if (activateObj instanceof Boolean) {
                activate = (Boolean) activateObj;
            } else {
                activate = Boolean.parseBoolean(activateObj.toString());
            }

            UserResponse updated = userService.toggleUserStatus(id, activate);

            log.info("PUT /api/users/{}/status - Successfully toggled to activate={}", id, activate);

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/users/{}/status - Failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PUT /api/users/{}/status - Unexpected error", id, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable String id) {

        log.info("DELETE /api/users/{} - Hard delete request (ADMIN)", id);

        try {
            userService.hardDeleteUser(id);

            log.info("DELETE /api/users/{} - Successfully hard-deleted", id);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.warn("DELETE /api/users/{} - Failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("DELETE /api/users/{} - Unexpected error", id, e);
            throw e;
        }
    }
}