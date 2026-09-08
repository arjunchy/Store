package com.ecommerce.backend.security;

import com.ecommerce.backend.config.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.trace("No Bearer token found for request: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);
        log.debug("Processing JWT token for request: {} {}", request.getMethod(), request.getRequestURI());

        if (jwtUtil.isRefreshToken(token)) {
            log.debug("Refresh token detected, skipping authentication for request: {} {} - refresh tokens cannot access API", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        // Ensure it's an access token before authenticating
        if (!jwtUtil.isAccessToken(token)) {
            log.debug("Token is not an access token, skipping authentication for {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String username = jwtUtil.extractUsername(token);
            log.debug("Extracted username from token: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                log.debug("Loaded UserDetails for username: {}", username);

                if (jwtUtil.isTokenValid(token, (com.ecommerce.backend.config.CustomUserDetails) userDetails)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("Successfully authenticated user: {} for request: {} {}", username, request.getMethod(), request.getRequestURI());
                } else {
                    log.debug("JWT token validation failed for user: {}", username);
                }
            } else if (username == null) {
                log.debug("Username extracted from JWT is null for request: {} {}", request.getMethod(), request.getRequestURI());
            }

        } catch (Exception e) {
            log.warn("JWT authentication failed for request: {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            log.debug("JWT authentication exception details", e);
        }

        filterChain.doFilter(request, response);
    }
}