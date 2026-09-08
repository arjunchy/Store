package com.ecommerce.backend.service.user;

import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.dto.request.UserUpdateRequest;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.mapper.UserMapper;
import com.ecommerce.backend.repository.AddressRepository;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.RefreshTokenRepository;
import com.ecommerce.backend.repository.ReviewRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.repository.WishlistRepository;
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

    private static final String SYSTEM_ADMIN_EMAIL = "ecommerce@gmail.com";

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

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

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
        if (SYSTEM_ADMIN_EMAIL.equalsIgnoreCase(email)) {
            log.warn("Delete blocked - cannot delete system admin: {}", email);
            throw new IllegalArgumentException("Cannot delete system admin account");
        }
        User user = userRepository.findActiveByEmail(email).orElseThrow(() -> {
            log.warn("Delete failed - user not found for email: {}", email);
            return new IllegalArgumentException("User not found");
        });
        if (SYSTEM_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            log.warn("Delete blocked - cannot delete system admin userId: {}", user.getUserId());
            throw new IllegalArgumentException("Cannot delete system admin account");
        }
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
        if (SYSTEM_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            log.warn("Change role blocked - cannot modify system admin: {} ({})", userId, user.getEmail());
            throw new IllegalArgumentException("Cannot change role of system admin");
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
        if (SYSTEM_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            log.warn("Toggle status blocked - cannot modify system admin: {} ({})", userId, user.getEmail());
            throw new IllegalArgumentException("Cannot deactivate system admin account");
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
        log.info("Admin hard deleting user with id: {} – will cascade delete all related data", userId);
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseGet(() -> userRepository.findById(userId).orElse(null));
        if (user == null) {
            log.warn("Hard delete failed - user not found with id: {}", userId);
            throw new IllegalArgumentException("User not found");
        }
        if (SYSTEM_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            log.warn("Hard delete blocked - cannot delete system admin: {} ({})", userId, user.getEmail());
            throw new IllegalArgumentException("Cannot delete system admin account");
        }
        // Admin hard delete – delete ALL data related to user, not just zero-dependency accounts
        // This includes carts, wishlists, addresses, reviews, orders (with items/payments/history), refresh tokens
        try {
            // 1. Refresh tokens
            try { refreshTokenRepository.deleteAllByUserId(userId); } catch (Exception e) { log.debug("refresh token delete failed for {}", userId, e); }

            // 2. Carts (and cart items via cascade/orphanRemoval)
            try {
                cartRepository.findByUserId(userId).ifPresent(cart -> {
                    try { cartRepository.delete(cart); cartRepository.flush(); } catch (Exception e) { log.warn("Failed to delete cart for user {}: {}", userId, e.getMessage()); }
                });
            } catch (Exception e) { log.debug("cart delete failed for {}", userId, e); }

            // 3. Wishlists
            try {
                var wishlists = wishlistRepository.findByUserIdWithProduct(userId);
                if (wishlists != null && !wishlists.isEmpty()) {
                    wishlistRepository.deleteAll(wishlists);
                    wishlistRepository.flush();
                }
            } catch (Exception e) { log.debug("wishlist delete failed for {}", userId, e); }

            // 4. Addresses
            try {
                var addresses = addressRepository.findByUserId(userId);
                if (addresses != null && !addresses.isEmpty()) {
                    addressRepository.deleteAll(addresses);
                    addressRepository.flush();
                }
            } catch (Exception e) { log.debug("address delete failed for {}", userId, e); }

            // 5. Reviews – use findAll + filter (no direct findByUserUserId)
            try {
                var allReviews = reviewRepository.findAll();
                var reviews = allReviews.stream().filter(r -> r.getUser() != null && userId.equals(r.getUser().getUserId())).toList();
                if (!reviews.isEmpty()) {
                    reviewRepository.deleteAll(reviews);
                    reviewRepository.flush();
                }
            } catch (Exception e) { log.debug("review delete failed for {}", userId, e); }

            // 6. Orders – delete all orders for user (cascade deletes order_items, payments, status_history)
            try {
                // Use paginated fetch to get all order ids
                var pageable = org.springframework.data.domain.PageRequest.of(0, 100);
                org.springframework.data.domain.Page<com.ecommerce.backend.entity.Order> page;
                do {
                    page = orderRepository.findByUserId(userId, pageable);
                    if (page.hasContent()) {
                        var orders = page.getContent();
                        // Ensure related collections are loaded for cascade
                        for (var o : orders) {
                            try { orderRepository.delete(o); } catch (Exception ex) { log.warn("Failed to delete order {} for user {}: {}", o.getId(), userId, ex.getMessage()); }
                        }
                        orderRepository.flush();
                    }
                    pageable = pageable.next();
                } while (page.hasNext());
            } catch (Exception e) { log.warn("Order cascade delete failed for user {}: {}", userId, e.getMessage()); }

            // 7. Finally hard delete user itself (native query bypasses soft-delete)
            userRepository.hardDeleteById(userId);
            log.info("Successfully hard-deleted user {} and all related data (admin cascade)", userId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to hard delete user with id: {} (admin cascade)", userId, e);
            throw new IllegalStateException("Hard delete failed: " + e.getMessage(), e);
        }
    }
}
