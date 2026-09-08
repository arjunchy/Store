package com.ecommerce.backend.service.auth;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.config.CustomUserDetailsService;
import com.ecommerce.backend.dto.request.AuthRequest;
import com.ecommerce.backend.dto.request.RefreshTokenRequest;
import com.ecommerce.backend.dto.response.AuthResponse;
import com.ecommerce.backend.entity.RefreshToken;
import com.ecommerce.backend.repository.RefreshTokenRepository;
import com.ecommerce.backend.security.JwtUtil;
import com.ecommerce.backend.service.auth.AuthService;
import com.ecommerce.backend.service.refreshtoken.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public AuthResponse login(AuthRequest request) {

        log.info("Login attempt for email: {}", request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        log.debug(
                "Authentication successful for email: {}",
                request.email()
        );

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService
                        .loadUserByUsername(request.email());

        log.debug(
                "Loaded user details for email: {}",
                request.email()
        );

        String accessToken =
                jwtUtil.generateAccessToken(userDetails);

        String refreshToken =
                jwtUtil.generateRefreshToken(userDetails);

        log.info(
                "Generated tokens for user: {}",
                request.email()
        );

        // Enforce max 5 active refresh tokens per user to prevent DB bloat
        long activeCount = refreshTokenRepository.countByUserUserIdAndRevokedFalse(userDetails.getUserId());
        if (activeCount >= 5) {
            // revoke oldest
            var oldTokens = refreshTokenRepository.findByUserUserId(userDetails.getUserId()).stream()
                .filter(rt -> !rt.getRevoked())
                .sorted((a,b) -> a.getExpiryDate().compareTo(b.getExpiryDate()))
                .toList();
            if (!oldTokens.isEmpty()) {
                var oldest = oldTokens.get(0);
                oldest.setRevoked(true);
                refreshTokenRepository.save(oldest);
                log.info("Revoked oldest refresh token for user {} due to limit", request.email());
            }
        }

        RefreshToken refreshTokenEntity = new RefreshToken();

        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setUser(userDetails.getUser());
        refreshTokenEntity.setRevoked(false);
        refreshTokenEntity.setExpiryDate(
                LocalDateTime.now().plusDays(7)
        );

        refreshTokenRepository.save(refreshTokenEntity);

        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElse(null);
        log.debug(
                "User role for {} is {}",
                request.email(),
                role
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                userDetails.getUsername(),
                role
        );
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {

        String refreshToken = request.refreshToken();

        log.info("Refresh token request received");
        log.debug("Validating refresh token");

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            log.warn(
                    "Invalid refresh token - not a refresh token type"
            );

            throw new IllegalArgumentException(
                    "Invalid refresh token"
            );
        }

        String username = jwtUtil.extractUsername(refreshToken);

        log.debug(
                "Extracted username from refresh token: {}",
                username
        );

        CustomUserDetails userDetails =
                (CustomUserDetails) customUserDetailsService
                        .loadUserByUsername(username);

        log.debug(
                "Loaded user details for refresh token user: {}",
                username
        );

        if (!jwtUtil.isTokenValidRefresh(
                refreshToken,
                userDetails
        )) {

            log.warn(
                    "Refresh token is invalid or expired for user: {}",
                    username
            );

            throw new IllegalArgumentException(
                    "Invalid or expired refresh token"
            );
        }

        log.info(
                "Refresh token validated successfully for user: {}",
                username
        );

        // Use pessimistic lock to prevent concurrent double refresh (token replay)
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenForUpdate(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
        if (refreshTokenEntity.getRevoked()) {
            log.warn("Refresh token has been revoked for user: {}", username);
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        if (refreshTokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token has expired in DB for user: {}", username);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        String newAccessToken =
                jwtUtil.generateAccessToken(userDetails);

        String newRefreshToken =
                jwtUtil.generateRefreshToken(userDetails);

        refreshTokenEntity.setRevoked(true);
        refreshTokenRepository.save(refreshTokenEntity);
        log.info("Revoked old refresh token for user: {}", username);

        RefreshToken newEntity = new RefreshToken();
        newEntity.setToken(newRefreshToken);
        newEntity.setUser(userDetails.getUser());
        newEntity.setRevoked(false);
        newEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(newEntity);
        log.info("Persisted new refresh token for user: {}", username);

        log.info(
                "Generated new token pair for user: {}",
                username
        );

        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElse(null);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                userDetails.getUsername(),
                role
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.info("Logout request received, revoking refresh token");
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Logout called without refresh token - clearing anyway");
            return;
        }
        try {
            refreshTokenService.revokeToken(refreshToken);
            log.info("Successfully revoked refresh token during logout");
        } catch (IllegalArgumentException e) {
            log.warn("Failed to revoke refresh token during logout - {} (tolerated)", e.getMessage());
            // Do not throw - logout should be idempotent
        } catch (Exception e) {
            log.error("Unexpected error during logout token revocation", e);
        }
    }
}