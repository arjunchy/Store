package com.ecommerce.backend.security;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.config.CustomUserDetailsService;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        User user = User.builder().userId("user-1").email("test@test.com").passwordHash("hash").userRole(UserRole.USER).build();
        userDetails = new CustomUserDetails(user);
    }

    @Test
    void doFilterInternal_noAuthHeader_passesThrough() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/products");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_nonBearerToken_passesThrough() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/products");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_refreshToken_skipsAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer refresh-token");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/refresh");
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.isAccessToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("test@test.com");
        when(customUserDetailsService.loadUserByUsername("test@test.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid("valid-token", userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
    }

    @Test
    void doFilterInternal_invalidToken_doesNotSetAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtUtil.isRefreshToken("bad-token")).thenReturn(false);
        when(jwtUtil.isAccessToken("bad-token")).thenReturn(true);
        when(jwtUtil.extractUsername("bad-token")).thenReturn("test@test.com");
        when(customUserDetailsService.loadUserByUsername("test@test.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid("bad-token", userDetails)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_nonAccessToken_skipsAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtUtil.isRefreshToken("some-token")).thenReturn(false);
        when(jwtUtil.isAccessToken("some-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_nullUsername_doesNotSetAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.isRefreshToken("token")).thenReturn(false);
        when(jwtUtil.isAccessToken("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_exceptionThrown_passesThrough() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.isRefreshToken("token")).thenReturn(false);
        when(jwtUtil.isAccessToken("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenThrow(new RuntimeException("JWT error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_existingAuth_doesNotOverride() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken("existing", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.isAccessToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("test@test.com");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
