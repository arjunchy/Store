package com.ecommerce.backend.service.refreshtoken;

import com.ecommerce.backend.entity.RefreshToken;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.RefreshTokenRepository;
import com.ecommerce.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public String createRefreshToken(String userId) {
        log.info("Creating refresh token for userId: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found with id: {}", userId);
                        return new IllegalArgumentException("User not found");
                    });

            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000);

            RefreshToken refreshToken = RefreshToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(expiryDate)
                    .revoked(false)
                    .build();

            RefreshToken saved = refreshTokenRepository.save(refreshToken);
            log.info("Created refresh token with id: {} for userId: {} expires at {}", saved.getId(), userId, expiryDate);
            return saved.getToken();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create refresh token for userId: {}", userId, e);
            throw e;
        }
    }

    @Transactional
    public void revokeToken(String token) {
        log.info("Revoking refresh token: {}", token);
        try {
            RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                    .orElseThrow(() -> {
                        log.warn("Refresh token not found: {}", token);
                        return new IllegalArgumentException("Refresh token not found");
                    });
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Successfully revoked refresh token: {}", token);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to revoke refresh token: {}", token, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValidToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.getRevoked() && rt.getExpiryDate().isAfter(LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllForUser(String userId) {
        java.util.List<RefreshToken> tokens = refreshTokenRepository.findByUserUserId(userId).stream()
                .filter(rt -> !rt.getRevoked())
                .toList();
        for (RefreshToken rt : tokens) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        }
        log.info("Revoked {} refresh tokens for userId: {}", tokens.size(), userId);
    }
}
