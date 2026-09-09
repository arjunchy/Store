package com.ecommerce.backend.service.refreshtoken;

import com.ecommerce.backend.entity.RefreshToken;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.RefreshTokenRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").build();
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
    }

    @Test
    void createRefreshToken_success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken rt = invocation.getArgument(0);
            rt.setId("rt-1");
            return rt;
        });

        String token = refreshTokenService.createRefreshToken("user-1");

        assertThat(token).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_userNotFound_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void revokeToken_success() {
        RefreshToken refreshToken = RefreshToken.builder().id("rt-1").token("abc-123").user(user).revoked(false).build();
        when(refreshTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any())).thenReturn(refreshToken);

        refreshTokenService.revokeToken("abc-123");

        assertThat(refreshToken.getRevoked()).isTrue();
    }

    @Test
    void revokeToken_notFound_throws() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.revokeToken("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token not found");
    }

    @Test
    void findValidToken_valid_returnsPresent() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id("rt-1").token("abc-123").user(user).revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(7)).build();
        when(refreshTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findValidToken("abc-123");

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("abc-123");
    }

    @Test
    void findValidToken_expired_returnsEmpty() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id("rt-1").token("abc-123").user(user).revoked(false)
                .expiryDate(LocalDateTime.now().minusDays(1)).build();
        when(refreshTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findValidToken("abc-123");

        assertThat(result).isEmpty();
    }

    @Test
    void findValidToken_revoked_returnsEmpty() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id("rt-1").token("abc-123").user(user).revoked(true)
                .expiryDate(LocalDateTime.now().plusDays(7)).build();
        when(refreshTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findValidToken("abc-123");

        assertThat(result).isEmpty();
    }

    @Test
    void findValidToken_notFound_returnsEmpty() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findValidToken("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void revokeAllForUser_success() {
        RefreshToken rt1 = RefreshToken.builder().id("rt-1").user(user).revoked(false).build();
        RefreshToken rt2 = RefreshToken.builder().id("rt-2").user(user).revoked(false).build();
        RefreshToken rt3 = RefreshToken.builder().id("rt-3").user(user).revoked(true).build();
        when(refreshTokenRepository.findByUserUserId("user-1")).thenReturn(List.of(rt1, rt2, rt3));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revokeAllForUser("user-1");

        assertThat(rt1.getRevoked()).isTrue();
        assertThat(rt2.getRevoked()).isTrue();
        assertThat(rt3.getRevoked()).isTrue();
    }

    @Test
    void revokeAllForUser_noTokens() {
        when(refreshTokenRepository.findByUserUserId("user-1")).thenReturn(List.of());

        refreshTokenService.revokeAllForUser("user-1");

        verify(refreshTokenRepository, never()).save(any());
    }
}
