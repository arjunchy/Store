package com.ecommerce.backend.service.user;

import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.dto.request.UserUpdateRequest;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.mapper.UserMapper;
import com.ecommerce.backend.repository.AddressRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.RefreshTokenRepository;
import com.ecommerce.backend.repository.ReviewRepository;
import com.ecommerce.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Override
    @Transactional
    public UserResponse register(UserRequest request) {
        log.info("Attempting to register user with email: {}", request.email());
        Optional<User> existingByEmail = userRepository.findByEmail(request.email());
        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (existing.getDeletedAt() == null) {
                log.warn("Registration failed - email already exists: {}", request.email());
                throw new IllegalArgumentException("Email already exists");
            }
            log.info("Re-activating soft-deleted account for email: {}", request.email());
            existing.setUsername(request.username());
            existing.setPasswordHash(passwordEncoder.encode(request.password()));
            // Security: always reset to USER on re-activation, prevent privilege elevation
            existing.setUserRole(UserRole.USER);
            existing.setDeletedAt(null);
            User savedUser = userRepository.save(existing);
            log.info("Successfully re-activated user with email: {} and id: {} (role reset to USER)", savedUser.getEmail(), savedUser.getUserId());
            return userMapper.toResponse(savedUser);
        }
        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserRole(UserRole.USER);
        User savedUser = userRepository.save(user);
        log.info("Successfully registered new user with email: {} and id: {}", savedUser.getEmail(), savedUser.getUserId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        log.debug("Fetching current user for email: {}", email);
        User user = userRepository.findActiveByEmail(email).orElseThrow(() -> {
            log.warn("User not found for email: {}", email);
            return new IllegalArgumentException("User not found");
        });
        log.debug("Found user with id: {} for email: {}", user.getUserId(), email);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        log.debug("Fetching all active users");
        List<User> users = userRepository.findAllActive();
        log.info("Retrieved {} active users", users.size());
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(
            String email,
            UserUpdateRequest request) {

        log.info("Attempting to update user with email: {}", email);
        User user = userRepository.findActiveByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Update failed - user not found for email: {}", email);
                    return new IllegalArgumentException("User not found");
                });

        log.debug("Updating username for user id: {} from {} to {}", user.getUserId(), user.getUsername(), request.username());
        user.setUsername(request.username());

        if (!user.getEmail().equals(request.email())) {
            log.debug("Email change requested for user id: {} from {} to {}", user.getUserId(), user.getEmail(), request.email());
            if (userRepository.existsActiveByEmail(request.email())) {
                log.warn("Update failed - new email already exists: {} for user id: {}", request.email(), user.getUserId());
                throw new IllegalArgumentException(
                        "Email already exists"
                );
            }
            user.setEmail(request.email());
            log.info("Email updated for user id: {} to {}", user.getUserId(), request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            log.debug("Updating password for user id: {}", user.getUserId());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            log.info("Password updated for user id: {}", user.getUserId());
        }
        User updatedUser = userRepository.save(user);
        log.info("Successfully updated user with id: {} and email: {}", updatedUser.getUserId(), updatedUser.getEmail());
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteCurrentUser(String email) {

        log.info("Attempting to soft-delete user with email: {}", email);
        User user = userRepository.findActiveByEmail(email).orElseThrow(() -> {
            log.warn("Delete failed - user not found for email: {}", email);
            return new IllegalArgumentException("User not found");
        });
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        try { refreshTokenRepository.deleteAllByUserId(user.getUserId()); } catch (Exception e) { log.warn("Failed to revoke tokens for soft-deleted user {}: {}", user.getUserId(), e.getMessage()); }
        log.info("Successfully soft-deleted user with id: {} and email: {} (tokens revoked)", user.getUserId(), email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(boolean includeDeleted) {
        log.debug("Fetching all users includeDeleted={}", includeDeleted);
        List<User> users;
        if (includeDeleted) {
            users = userRepository.findAllIncludingDeleted();
            log.info("Retrieved {} users (including deleted)", users.size());
        } else {
            users = userRepository.findAllActive();
            log.info("Retrieved {} active users", users.size());
        }
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsersPaginated(boolean includeDeleted, String search, Pageable pageable) {
        log.debug("Fetching paginated users includeDeleted={} search={} pageable={}", includeDeleted, search, pageable);
        if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be <=100");
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
        if (normalizedSearch != null && normalizedSearch.length() > 100) normalizedSearch = normalizedSearch.substring(0,100);
        Page<User> page = userRepository.findByAdminFilterNative(normalizedSearch, includeDeleted, pageable);
        log.info("Retrieved {} users page {} of {} (includeDeleted={} search={})", page.getNumberOfElements(), page.getNumber(), page.getTotalPages(), includeDeleted, normalizedSearch);
        return page.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse changeUserRole(String userId, UserRole newRole) {
        log.info("Changing role for userId: {} to {}", userId, newRole);
        if (newRole == null) {
            log.warn("Change role failed - newRole is null for userId: {}", userId);
            throw new IllegalArgumentException("Role is required");
        }
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseGet(() -> userRepository.findById(userId).orElse(null));
        if (user == null) {
            log.warn("Change role failed - user not found with id: {}", userId);
            throw new IllegalArgumentException("User not found");
        }
        if (user.getDeletedAt() != null) {
            log.warn("Change role failed - user is deactivated: {}", userId);
            throw new IllegalArgumentException("User is deactivated");
        }
        log.debug("Updating role for userId: {} from {} to {}", userId, user.getUserRole(), newRole);
        user.setUserRole(newRole);
        User saved = userRepository.save(user);
        log.info("Successfully changed role for userId: {} to {}", userId, newRole);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(String userId, boolean activate) {
        log.info("Toggling status for userId: {} activate={}", userId, activate);
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseGet(() -> userRepository.findById(userId).orElse(null));
        if (user == null) {
            log.warn("Toggle status failed - user not found with id: {}", userId);
            throw new IllegalArgumentException("User not found");
        }
        if (activate) {
            if (user.getDeletedAt() == null) {
                log.warn("Activate failed - user already active: {}", userId);
                throw new IllegalArgumentException("User is already active");
            }
            user.setDeletedAt(null);
            User saved = userRepository.save(user);
            log.info("Successfully activated user with id: {}", userId);
            return userMapper.toResponse(saved);
        } else {
            if (user.getDeletedAt() != null) {
                log.warn("Deactivate failed - user already deactivated: {}", userId);
                throw new IllegalArgumentException("User is already deactivated");
            }
            user.setDeletedAt(LocalDateTime.now());
            User saved = userRepository.save(user);
            try { refreshTokenRepository.deleteAllByUserId(userId); } catch (Exception e) { log.warn("Failed to revoke tokens for deactivated user {}: {}", userId, e.getMessage()); }
            log.info("Successfully deactivated user with id: {} (tokens revoked)", userId);
            return userMapper.toResponse(saved);
        }
    }

    @Override
    @Transactional
    public void hardDeleteUser(String userId) {
        log.info("Hard deleting user with id: {}", userId);
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseGet(() -> userRepository.findById(userId).orElse(null));
        if (user == null) {
            log.warn("Hard delete failed - user not found with id: {}", userId);
            throw new IllegalArgumentException("User not found");
        }
        boolean hasOrders = false;
        boolean hasReviews = false;
        boolean hasAddresses = false;
        try { hasOrders = orderRepository.existsByUserUserId(userId); } catch (Exception e) { log.debug("existsByUserUserId check failed", e); }
        try { hasReviews = reviewRepository.existsByUserUserId(userId); } catch (Exception e) { log.debug("review exists check failed", e); }
        try { hasAddresses = addressRepository.existsByUserUserId(userId); } catch (Exception e) { log.debug("address check failed", e); }
        if (hasOrders || hasReviews || hasAddresses) {
            String reason = "User has dependencies:" +
                    (hasOrders ? " orders" : "") +
                    (hasReviews ? " reviews" : "") +
                    (hasAddresses ? " addresses" : "") +
                    " — deactivate instead; hard delete only for zero-dependency accounts";
            log.warn("Hard delete blocked for user {}: {}", userId, reason);
            throw new IllegalStateException(reason);
        }
        try {
            try { refreshTokenRepository.deleteAllByUserId(userId); } catch (Exception ignored) {}
            userRepository.hardDeleteById(userId);
            log.info("Successfully hard-deleted user with id: {}", userId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to hard delete user with id: {}", userId, e);
            throw new IllegalStateException("Hard delete failed: " + e.getMessage(), e);
        }
    }
}
