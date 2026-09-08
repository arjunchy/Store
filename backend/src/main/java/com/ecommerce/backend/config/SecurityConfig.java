package com.ecommerce.backend.config;

import com.ecommerce.backend.security.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("Creating BCryptPasswordEncoder bean");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {

        log.debug("Creating DaoAuthenticationProvider bean");
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.debug("Creating AuthenticationManager bean");
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow via env CORS_ALLOWED_ORIGINS (comma-separated) fallback to localhost for dev
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (envOrigins != null && !envOrigins.isBlank()) {
            config.setAllowedOrigins(List.of(envOrigins.split(",")));
        } else {
            config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"));
        }
        // NOTE: credentials are enabled, so we intentionally use exact allowed origins
        // rather than wildcard patterns (a wildcard like *.vercel.app would let any
        // attacker-controlled subdomain make credentialed cross-origin requests).
        // Set CORS_ALLOWED_ORIGINS=<comma-separated origins> for production.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring SecurityFilterChain");
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> {
                    log.debug("Disabling CSRF");
                    csrf.disable();
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> {
                    log.debug("Configuring authorization rules");
                    auth
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                                // Admin product stats must be authenticated before wildcard
                                .requestMatchers(HttpMethod.GET, "/api/products/stats").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                                .requestMatchers("/uploads/**").permitAll()
                                .anyRequest()
                                .authenticated();
                        }
                )
                .exceptionHandling(ex -> {
                    ex.authenticationEntryPoint((req, res, authEx) -> {
                        res.setStatus(401);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"timestamp\":\""+java.time.LocalDateTime.now()+"\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"path\":\""+req.getRequestURI()+"\"}");
                    });
                    ex.accessDeniedHandler((req, res, accessEx) -> {
                        res.setStatus(403);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"timestamp\":\""+java.time.LocalDateTime.now()+"\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\",\"path\":\""+req.getRequestURI()+"\"}");
                    });
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("SecurityFilterChain configured successfully");
        return http.build();
    }
}
