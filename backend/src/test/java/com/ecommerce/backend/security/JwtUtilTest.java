package com.ecommerce.backend.security;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private CustomUserDetails userDetails;
    private CustomUserDetails otherUserDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-that-is-long-enough-for-hs256-testing-1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L);

        User user = User.builder()
                .userId("uuid-1")
                .username("john")
                .email("john@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.USER)
                .build();
        userDetails = new CustomUserDetails(user);

        User other = User.builder()
                .userId("uuid-2")
                .username("jane")
                .email("jane@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.USER)
                .build();
        otherUserDetails = new CustomUserDetails(other);
    }

    @Test
    void generateAccessToken_andExtractUsername() {
        String token = jwtUtil.generateAccessToken(userDetails);

        assertThat(token).isNotBlank();
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("john@example.com");
    }

    @Test
    void generateRefreshToken_isRefreshTokenTrue() {
        String token = jwtUtil.generateRefreshToken(userDetails);

        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    void generateAccessToken_isRefreshTokenFalse() {
        String token = jwtUtil.generateAccessToken(userDetails);

        assertThat(jwtUtil.isRefreshToken(token)).isFalse();
    }

    @Test
    void isTokenValid_trueForCorrectUser() {
        String token = jwtUtil.generateAccessToken(userDetails);

        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_falseForWrongUser() {
        String token = jwtUtil.generateAccessToken(userDetails);

        assertThat(jwtUtil.isTokenValid(token, otherUserDetails)).isFalse();
    }

    @Test
    void isTokenValid_falseForInvalidToken() {
        assertThat(jwtUtil.isTokenValid("invalid.token.here", userDetails)).isFalse();
    }

    @Test
    void isTokenValid_falseForTamperedToken() {
        String token = jwtUtil.generateAccessToken(userDetails);
        String tampered = token.substring(0, token.length() - 2) + "ab";

        assertThat(jwtUtil.isTokenValid(tampered, userDetails)).isFalse();
    }

    @Test
    void extractUsername_withInvalidToken_throws() {
        try {
            jwtUtil.extractUsername("invalid.token");
            assertThat(false).as("should have thrown").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void isRefreshToken_falseForInvalidToken() {
        assertThat(jwtUtil.isRefreshToken("invalid.token.here")).isFalse();
    }

    @Test
    void isRefreshToken_falseForAccessToken() {
        String access = jwtUtil.generateAccessToken(userDetails);
        assertThat(jwtUtil.isRefreshToken(access)).isFalse();
    }

    @Test
    void generateTokens_differentTypesHaveDifferentClaims() {
        String access = jwtUtil.generateAccessToken(userDetails);
        String refresh = jwtUtil.generateRefreshToken(userDetails);

        assertThat(access).isNotEqualTo(refresh);
        assertThat(jwtUtil.isRefreshToken(access)).isFalse();
        assertThat(jwtUtil.isRefreshToken(refresh)).isTrue();
        assertThat(jwtUtil.extractUsername(access)).isEqualTo(jwtUtil.extractUsername(refresh));
    }

    @Test
    void getSigningKey_shortSecret_handledViaSha256() {
        JwtUtil shortSecretUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortSecretUtil, "secret", "short");
        ReflectionTestUtils.setField(shortSecretUtil, "expiration", 900000L);
        ReflectionTestUtils.setField(shortSecretUtil, "refreshExpiration", 604800000L);

        User user = User.builder().userId("1").username("u").email("a@b.com").passwordHash("h").userRole(UserRole.USER).build();
        CustomUserDetails details = new CustomUserDetails(user);

        String token = shortSecretUtil.generateAccessToken(details);
        assertThat(token).isNotBlank();
        assertThat(shortSecretUtil.isTokenValid(token, details)).isTrue();
        assertThat(shortSecretUtil.extractUsername(token)).isEqualTo("a@b.com");
    }

    @Test
    void tokenExpired_returnsFalseForIsTokenValid() throws Exception {
        JwtUtil shortExpiryUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpiryUtil, "secret", "test-secret-key-that-is-long-enough-for-hs256-testing-1234567890");
        ReflectionTestUtils.setField(shortExpiryUtil, "expiration", 1L); // 1ms
        ReflectionTestUtils.setField(shortExpiryUtil, "refreshExpiration", 604800000L);

        User user = User.builder().userId("1").username("u").email("a@b.com").passwordHash("h").userRole(UserRole.USER).build();
        CustomUserDetails details = new CustomUserDetails(user);
        String token = shortExpiryUtil.generateAccessToken(details);
        Thread.sleep(10);
        assertThat(shortExpiryUtil.isTokenValid(token, details)).isFalse();
    }
}
