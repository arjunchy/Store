package com.ecommerce.backend.service.auth;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.config.CustomUserDetailsService;
import com.ecommerce.backend.dto.request.AuthRequest;
import com.ecommerce.backend.dto.response.AuthResponse;
import com.ecommerce.backend.dto.request.RefreshTokenRequest;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.entity.RefreshToken;
import com.ecommerce.backend.repository.RefreshTokenRepository;
import com.ecommerce.backend.security.JwtUtil;
import com.ecommerce.backend.service.refreshtoken.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private CustomUserDetails userDetails;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("uuid-1")
                .username("john")
                .email("john@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.USER)
                .build();
        userDetails = new CustomUserDetails(user);
    }

    // --- login ---

    @Test
    void login_success() {
        AuthRequest request = new AuthRequest("john@example.com", "password123");
        when(customUserDetailsService.doesUserExist("john@example.com")).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.email()).isEqualTo("john@example.com"); // CustomUserDetails.getUsername() returns email
        assertThat(response.role()).isEqualTo("ROLE_USER");
    }

    @Test
    void login_success_adminRole() {
        User adminUser = User.builder().userId("2").username("admin").email("admin@example.com").passwordHash("h").userRole(UserRole.ADMIN).build();
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        AuthRequest request = new AuthRequest("admin@example.com", "pass");
        when(customUserDetailsService.doesUserExist("admin@example.com")).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("admin@example.com")).thenReturn(adminDetails);
        when(jwtUtil.generateAccessToken(adminDetails)).thenReturn("a");
        when(jwtUtil.generateRefreshToken(adminDetails)).thenReturn("r");

        AuthResponse response = authService.login(request);

        assertThat(response.role()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void login_unknownEmail_throwsUserNotFound() {
        AuthRequest request = new AuthRequest("ghost@example.com", "whatever");
        when(customUserDetailsService.doesUserExist("ghost@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.ecommerce.backend.exception.UserNotFoundException.class)
                .hasMessageContaining("No account found");

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void login_badCredentials_propagatesException() {
        AuthRequest request = new AuthRequest("john@example.com", "wrong");
        when(customUserDetailsService.doesUserExist("john@example.com")).thenReturn(true);
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid password");

        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void login_authenticatesWithEmailAndPassword() {
        AuthRequest request = new AuthRequest("john@example.com", "password123");
        when(customUserDetailsService.doesUserExist(anyString())).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(any())).thenReturn("a");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("r");

        authService.login(request);

        verify(authenticationManager).authenticate(argThat(auth ->
                ((UsernamePasswordAuthenticationToken) auth).getPrincipal().equals("john@example.com")
                        && ((UsernamePasswordAuthenticationToken) auth).getCredentials().equals("password123")
        ));
    }

    // --- refresh ---

    @Test
    void refresh_success() {
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        RefreshToken dbToken = RefreshToken.builder()
                .id("1")
                .token(refreshToken)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .user(user)
                .build();

        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("john@example.com");
        when(customUserDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValidRefresh(refreshToken, userDetails)).thenReturn(true);
        when(refreshTokenRepository.findByTokenForUpdate(refreshToken)).thenReturn(Optional.of(dbToken));
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("new-refresh");

        AuthResponse response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.email()).isEqualTo("john@example.com");
        verify(jwtUtil).isRefreshToken(refreshToken);
        verify(jwtUtil).extractUsername(refreshToken);
        verify(jwtUtil).isTokenValidRefresh(refreshToken, userDetails);
        verify(refreshTokenRepository).save(dbToken);
        assertThat(dbToken.getRevoked()).isTrue();
    }

    @Test
    void refresh_throwsWhenNotRefreshToken() {
        String token = "access-token";
        RefreshTokenRequest request = new RefreshTokenRequest(token);
        when(jwtUtil.isRefreshToken(token)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).isTokenValidRefresh(anyString(), any());
    }

    @Test
    void refresh_throwsWhenTokenInvalidOrExpired() {
        String token = "expired-refresh";
        RefreshTokenRequest request = new RefreshTokenRequest(token);
        when(jwtUtil.isRefreshToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("john@example.com");
        when(customUserDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValidRefresh(token, userDetails)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");

        verify(jwtUtil).isTokenValidRefresh(token, userDetails);
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void refresh_propagatesWhenUserNotFound() {
        String token = "valid-refresh";
        RefreshTokenRequest request = new RefreshTokenRequest(token);
        when(jwtUtil.isRefreshToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("missing@example.com");
        when(customUserDetailsService.loadUserByUsername("missing@example.com"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void refresh_generatesBothTokensOnSuccess() {
        String token = "valid-refresh";
        RefreshTokenRequest request = new RefreshTokenRequest(token);

        RefreshToken dbToken = RefreshToken.builder()
                .id("2")
                .token(token)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .user(user)
                .build();

        when(jwtUtil.isRefreshToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("john@example.com");
        when(customUserDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValidRefresh(token, userDetails)).thenReturn(true);
        when(refreshTokenRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(dbToken));
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("a");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("r");

        authService.refresh(request);

        verify(jwtUtil).generateAccessToken(userDetails);
        verify(jwtUtil).generateRefreshToken(userDetails);
        verify(refreshTokenRepository).save(dbToken);
    }

    @Test
    void refresh_extractUsernameCalledWithCorrectToken() {
        String token = "my-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(token);

        RefreshToken dbToken = RefreshToken.builder()
                .id("3")
                .token(token)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .user(user)
                .build();

        when(jwtUtil.isRefreshToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("john@example.com");
        when(customUserDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtUtil.isTokenValidRefresh(anyString(), any())).thenReturn(true);
        when(refreshTokenRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(dbToken));
        when(jwtUtil.generateAccessToken(any())).thenReturn("a");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("r");

        authService.refresh(request);

        verify(jwtUtil).extractUsername(eq(token));
    }
}
